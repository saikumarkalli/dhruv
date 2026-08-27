#!/usr/bin/env python3
"""Generate supabase/SCHEMA.md from supabase/schemas/ (ADR-0032, ADR-0033).

Two modes, both pure static SQL parsing — no live database connection, no Docker,
no supabase CLI required:

  docs   (default) — parse supabase/schemas/**/*.sql and write supabase/SCHEMA.md.
                      `--check` instead exits 1 if the committed SCHEMA.md is stale
                      (used by CI so the doc can't silently rot, same pattern as
                      the JaCoCo coverage floor).

  equiv  --check    — the ADR-0032 "equivalence guard". Parses supabase/schemas/
                      (declarative, current-state) and supabase/migrations/*.sql
                      concatenated in order (the actual executed history), reduces
                      both to the same normalized {schemas, tables, functions,
                      extensions} shape, and fails if they disagree. This is what
                      makes the per-object files in schemas/ trustworthy
                      documentation instead of a second copy that quietly goes stale.

This is a pragmatic regex-based parser, not a SQL grammar — it understands exactly
the subset of DDL this repo's migrations use (create schema, create table/index,
alter table ... enable row level security, create policy, create or replace
function, create extension). It is not a general-purpose SQL parser and is not
meant to become one; anything it can't confidently parse is reported, not guessed at.

ADR-0033: objects are namespaced per app Postgres schema (`finance.holdings`, not
`public.holdings`) — every name below is tracked and rendered as its fully
qualified `schema.object` form, defaulting to `public` when a statement carries no
explicit schema prefix (matches Postgres' own default resolution).
"""
from __future__ import annotations

import argparse
import glob
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

SCHEMAS_DIR = Path("supabase/schemas")
MIGRATIONS_DIR = Path("supabase/migrations")
SCHEMA_MD = Path("supabase/SCHEMA.md")

DEFAULT_SCHEMA = "public"

# Mirrors config.toml's [db.migrations].schema_paths glob order — dependency order
# matters for the generated doc's section ordering, not for parsing correctness.
# Adding a new app schema means adding its own block here, same as `finance`'s.
SCHEMA_GLOBS = [
    "00_extensions.sql",
    "finance/00_schema.sql",
    "finance/10_tables/*.sql",
    "finance/20_views/*.sql",
    "finance/30_functions/*.sql",
    "public/30_functions/*.sql",
]


def _qualify(schema: str | None, name: str) -> str:
    return f"{schema or DEFAULT_SCHEMA}.{name}"


@dataclass
class Column:
    name: str
    type: str
    constraints: str

    def key(self) -> tuple[str, str]:
        # Name + type only. Constraint TEXT is deliberately excluded: the same constraint is
        # legitimately spelled two ways depending on which side declares it —
        #   declarative:  invested_paise bigint check (invested_paise >= 0)
        #   migration:    alter table ... add constraint x check (invested_paise >= 0)
        # Comparing the raw strings would fail forever on a schema that is in fact identical, and a
        # guard that cries wolf is worse than one with a documented gap. CHECK expressions are
        # instead compared per-table as a normalized set (see Table.checks).
        return (self.name, _normalize_sql(self.type))


@dataclass
class Policy:
    name: str
    command: str  # select | insert | update | delete


@dataclass
class Table:
    qualified_name: str
    columns: list[Column] = field(default_factory=list)
    indexes: list[str] = field(default_factory=list)
    rls_enabled: bool = False
    policies: list[Policy] = field(default_factory=list)
    checks: set[str] = field(default_factory=set)

    def signature(self):
        return (
            # Order-insensitive. Postgres column order carries no semantics, and a
            # `db diff`-generated migration APPENDS new columns while the declarative file
            # declares them in place — so the two sides routinely differ in order while being
            # identical schemas.
            frozenset(c.key() for c in self.columns),
            self.rls_enabled,
            tuple(sorted((p.name, p.command) for p in self.policies)),
            frozenset(self.checks),
        )


@dataclass
class Function:
    qualified_name: str
    args: str
    returns: str
    security: str  # definer | invoker | unspecified

    def signature(self):
        return (_normalize_sql(self.args), _normalize_sql(self.returns), self.security)


@dataclass
class Schema:
    app_schemas: set[str] = field(default_factory=set)
    extensions: set[str] = field(default_factory=set)
    tables: dict[str, Table] = field(default_factory=dict)
    functions: dict[str, Function] = field(default_factory=dict)


def _normalize_sql(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip().lower()


REFERENCES_SCHEMA_RE = re.compile(r"references\s+\w+\.(\w+)", re.IGNORECASE)


def _normalize_constraints(text: str) -> str:
    """Like _normalize_sql, but additionally strips the schema qualifier off a `references
    x.y` target before comparing. Postgres resolves an FK's target by OID, not by the
    qualifier text recorded at CREATE time — after the target table is later moved via
    `ALTER TABLE ... SET SCHEMA` (ADR-0033), older migrations still contain the pre-move
    qualifier verbatim even though the constraint's real target has already moved with it.
    Stripping the qualifier avoids flagging that staleness as drift while still catching a
    genuine change of *which table* is referenced."""
    return REFERENCES_SCHEMA_RE.sub(r"references \1", _normalize_sql(text))


def _split_top_level_commas(body: str) -> list[str]:
    """Split a column-def block on commas that aren't nested inside ( )."""
    parts: list[str] = []
    depth = 0
    current: list[str] = []
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(ch)
    if current:
        parts.append("".join(current))
    return [p.strip() for p in parts if p.strip()]


def _strip_comments(sql: str) -> str:
    sql = re.sub(r"--[^\n]*", "", sql)
    return sql


def _extract_checks(fragment: str) -> list[str]:
    """Pull every CHECK expression out of a DDL fragment, normalized for comparison.

    Balanced-paren scan rather than a regex, because a CHECK body routinely contains nested
    parens — `check (sector in ('BANK', 'GOLD'))`, `check (length(btrim(name)) between 1 and 120)`.
    The constraint NAME is deliberately not captured: the declarative side spells these inline and
    unnamed, the migration side names them, and they describe the same rule either way.
    """
    checks: list[str] = []
    for match in re.finditer(r"\bcheck\s*\(", fragment, re.IGNORECASE):
        start = match.end() - 1  # position of the opening paren
        depth = 0
        for i in range(start, len(fragment)):
            char = fragment[i]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    checks.append(_normalize_sql(fragment[start + 1 : i]))
                    break
    return checks


SCHEMA_CREATE_RE = re.compile(
    r"create schema if not exists\s+(\w+)",
    re.IGNORECASE,
)
TABLE_RE = re.compile(
    r"create table if not exists\s+(?:(\w+)\.)?(\w+)\s*\((.*?)\)\s*;",
    re.IGNORECASE | re.DOTALL,
)
INDEX_RE = re.compile(
    r"create (?:unique )?index if not exists\s+(\w+)\s+on\s+(?:(\w+)\.)?(\w+)",
    re.IGNORECASE,
)
RLS_RE = re.compile(
    r"alter table\s+(?:(\w+)\.)?(\w+)\s+enable row level security",
    re.IGNORECASE,
)
SET_SCHEMA_RE = re.compile(
    r"alter table\s+(?:(\w+)\.)?(\w+)\s+set schema\s+(\w+)",
    re.IGNORECASE,
)
# A migration extends an existing table in place; the declarative file declares the column inside
# `create table`. Without these two rules the executed side simply never sees the column and every
# extended table reads as permanent drift. ADR-0033 hit the same class of gap and resolved it the
# same way, by teaching this parser the statement it did not know.
ADD_COLUMN_RE = re.compile(
    r"alter table\s+(?:if exists\s+)?(?:(\w+)\.)?(\w+)\s+"
    r"add column\s+(?:if not exists\s+)?(\w+)\s+([^;]+);",
    re.IGNORECASE | re.DOTALL,
)
ADD_TABLE_CONSTRAINT_RE = re.compile(
    r"alter table\s+(?:if exists\s+)?(?:(\w+)\.)?(\w+)\s+add constraint\s+\w+\s+([^;]+);",
    re.IGNORECASE | re.DOTALL,
)
POLICY_RE = re.compile(
    r'create policy\s+"([^"]+)"\s+on\s+(?:(\w+)\.)?(\w+)\s+for\s+(select|insert|update|delete)',
    re.IGNORECASE,
)
FUNCTION_RE = re.compile(
    r"create (?:or replace )?function\s+(?:(\w+)\.)?(\w+)\s*\((.*?)\)\s*"
    r"returns\s+(\w+)(.*?)(?:as\s+\$\$.*?\$\$\s*;|;)",
    re.IGNORECASE | re.DOTALL,
)
EXTENSION_RE = re.compile(
    r"create extension if not exists\s+(\w+)",
    re.IGNORECASE,
)


def parse_sql(sql: str, schema: Schema) -> None:
    sql = _strip_comments(sql)

    for match in SCHEMA_CREATE_RE.finditer(sql):
        schema.app_schemas.add(match.group(1))

    for match in EXTENSION_RE.finditer(sql):
        schema.extensions.add(match.group(1))

    for match in TABLE_RE.finditer(sql):
        schema_name, name, body = match.group(1), match.group(2), match.group(3)
        qualified = _qualify(schema_name, name)
        table = schema.tables.setdefault(qualified, Table(qualified_name=qualified))
        table.columns = []
        # Covers both spellings inside a create-table body: inline column CHECKs and a table-level
        # `constraint <name> check (...)` row.
        table.checks.update(_extract_checks(body))
        for col_def in _split_top_level_commas(body):
            tokens = col_def.split(None, 1)
            if not tokens:
                continue
            col_name = tokens[0]
            rest = tokens[1] if len(tokens) > 1 else ""
            # type is the leading run of non-constraint tokens; constraints start at
            # the first known keyword (not, default, references, primary, check, unique).
            constraint_kw = re.search(
                r"\b(not null|default|references|primary key|check|unique)\b",
                rest,
                re.IGNORECASE,
            )
            if constraint_kw:
                col_type = rest[: constraint_kw.start()].strip()
                constraints = rest[constraint_kw.start():].strip()
            else:
                col_type = rest.strip()
                constraints = ""
            table.columns.append(Column(col_name, col_type, constraints))

    for match in INDEX_RE.finditer(sql):
        idx_name, schema_name, table_name = match.group(1), match.group(2), match.group(3)
        qualified = _qualify(schema_name, table_name)
        table = schema.tables.setdefault(qualified, Table(qualified_name=qualified))
        if idx_name not in table.indexes:
            table.indexes.append(idx_name)

    for match in RLS_RE.finditer(sql):
        schema_name, table_name = match.group(1), match.group(2)
        qualified = _qualify(schema_name, table_name)
        schema.tables.setdefault(qualified, Table(qualified_name=qualified)).rls_enabled = True

    # ALTER TABLE ... SET SCHEMA is metadata-only in Postgres — data, indexes, FKs,
    # RLS enablement and policies all move with it. Rename the tracked entry's key
    # (not a fresh Table) so everything already accumulated for the old qualified
    # name (from an earlier-processed file) carries over. This only ever fires
    # against the executed migrations/ history — schemas/ declares the post-move
    # name directly and has no ALTER statements to match.
    for match in SET_SCHEMA_RE.finditer(sql):
        old_schema_name, table_name, new_schema_name = (
            match.group(1),
            match.group(2),
            match.group(3),
        )
        old_qualified = _qualify(old_schema_name, table_name)
        new_qualified = _qualify(new_schema_name, table_name)
        existing = schema.tables.pop(old_qualified, None)
        if existing is None:
            existing = Table(qualified_name=new_qualified)
        existing.qualified_name = new_qualified
        schema.tables[new_qualified] = existing

    # Must run AFTER both TABLE_RE (which resets a table's column list) and SET_SCHEMA_RE (which
    # re-keys a moved table), so an ALTER lands on the table entry those two just established.
    for match in ADD_COLUMN_RE.finditer(sql):
        schema_name, table_name, col_name, rest = match.groups()
        qualified = _qualify(schema_name, table_name)
        table = schema.tables.setdefault(qualified, Table(qualified_name=qualified))
        if any(c.name == col_name for c in table.columns):
            continue  # `add column if not exists` replayed; not a second column
        constraint_kw = re.search(
            r"\b(not null|default|references|primary key|check|unique)\b", rest, re.IGNORECASE
        )
        col_type = rest[: constraint_kw.start()].strip() if constraint_kw else rest.strip()
        constraints = rest[constraint_kw.start():].strip() if constraint_kw else ""
        table.columns.append(Column(col_name, col_type, constraints))
        table.checks.update(_extract_checks(rest))

    for match in ADD_TABLE_CONSTRAINT_RE.finditer(sql):
        schema_name, table_name, body = match.groups()
        qualified = _qualify(schema_name, table_name)
        table = schema.tables.setdefault(qualified, Table(qualified_name=qualified))
        # Only CHECKs are compared — see Column.key(). A named UNIQUE/PK/FK added by ALTER has no
        # comparable inline spelling on the declarative side, so folding it in would reintroduce
        # exactly the false positives this change removes.
        table.checks.update(_extract_checks(body))

    for match in POLICY_RE.finditer(sql):
        policy_name, schema_name, table_name, command = (
            match.group(1),
            match.group(2),
            match.group(3),
            match.group(4),
        )
        qualified = _qualify(schema_name, table_name)
        table = schema.tables.setdefault(qualified, Table(qualified_name=qualified))
        table.policies.append(Policy(policy_name, command.lower()))

    for match in FUNCTION_RE.finditer(sql):
        schema_name, name, args, returns, tail = (
            match.group(1),
            match.group(2),
            match.group(3),
            match.group(4),
            match.group(5),
        )
        if re.search(r"security\s+definer", tail, re.IGNORECASE):
            security = "definer"
        elif re.search(r"security\s+invoker", tail, re.IGNORECASE):
            security = "invoker"
        else:
            security = "unspecified"
        qualified = _qualify(schema_name, name)
        schema.functions[qualified] = Function(qualified, args.strip(), returns.strip(), security)


def load_schema_from_dir() -> Schema:
    schema = Schema()
    for pattern in SCHEMA_GLOBS:
        for path in sorted(glob.glob(str(SCHEMAS_DIR / pattern))):
            parse_sql(Path(path).read_text(encoding="utf-8"), schema)
    return schema


def load_schema_from_migrations() -> Schema:
    schema = Schema()
    for path in sorted(MIGRATIONS_DIR.glob("*.sql")):
        parse_sql(path.read_text(encoding="utf-8"), schema)
    return schema


def _group_by_schema(qualified_names: list[str]) -> dict[str, list[str]]:
    grouped: dict[str, list[str]] = {}
    for qualified in qualified_names:
        schema_name, _, obj_name = qualified.partition(".")
        grouped.setdefault(schema_name, []).append(obj_name)
    for names in grouped.values():
        names.sort()
    return grouped


def render_markdown(schema: Schema) -> str:
    lines = [
        "# Dhruv Tracker — Schema Reference",
        "",
        "> **Generated file — do not hand-edit.** Produced by `scripts/db/gen_schema_docs.py` from"
        " `supabase/schemas/`. Regenerate after any schema change:"
        " `python scripts/db/gen_schema_docs.py`. CI (`supabase-migrate.yml`) fails the build if"
        " this file is stale (ADR-0032). Objects are grouped by Postgres schema — one per app"
        " (ADR-0033); `public` holds cross-app orchestration only.",
        "",
        "## Postgres schemas",
        "",
    ]
    for app_schema in sorted(schema.app_schemas):
        lines.append(f"- `{app_schema}`")
    if not schema.app_schemas:
        lines.append("_none declared via `create schema`_")
    lines.append("")

    lines.append("## Extensions")
    lines.append("")
    for ext in sorted(schema.extensions):
        lines.append(f"- `{ext}`")
    if not schema.extensions:
        lines.append("_none_")
    lines.append("")

    lines.append("## Tables")
    lines.append("")
    tables_by_schema = _group_by_schema(list(schema.tables))
    for schema_name in sorted(tables_by_schema):
        lines.append(f"### Schema `{schema_name}`")
        lines.append("")
        for obj_name in tables_by_schema[schema_name]:
            table = schema.tables[_qualify(schema_name, obj_name)]
            lines.append(f"#### `{table.qualified_name}`")
            lines.append("")
            lines.append(f"RLS: {'**enabled**' if table.rls_enabled else '⚠️ not enabled'}")
            lines.append("")
            lines.append("| Column | Type | Constraints |")
            lines.append("|---|---|---|")
            for col in table.columns:
                lines.append(f"| `{col.name}` | `{col.type}` | {col.constraints or '—'} |")
            lines.append("")
            if table.indexes:
                lines.append("Indexes: " + ", ".join(f"`{i}`" for i in table.indexes))
                lines.append("")
            if table.policies:
                lines.append("| Policy | Command |")
                lines.append("|---|---|")
                for pol in table.policies:
                    lines.append(f"| `{pol.name}` | {pol.command} |")
            else:
                lines.append("_no policies defined_")
            lines.append("")

    lines.append("## Functions")
    lines.append("")
    functions_by_schema = _group_by_schema(list(schema.functions))
    for schema_name in sorted(functions_by_schema):
        lines.append(f"### Schema `{schema_name}`")
        lines.append("")
        for obj_name in functions_by_schema[schema_name]:
            fn = schema.functions[_qualify(schema_name, obj_name)]
            lines.append(f"#### `{fn.qualified_name}({fn.args})`")
            lines.append("")
            lines.append(f"Returns `{fn.returns}` · security **{fn.security}**")
            lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def cmd_docs(check: bool) -> int:
    schema = load_schema_from_dir()
    rendered = render_markdown(schema)
    if check:
        if not SCHEMA_MD.exists() or SCHEMA_MD.read_text(encoding="utf-8") != rendered:
            print(f"❌ {SCHEMA_MD} is stale — run: python3 scripts/db/gen_schema_docs.py", file=sys.stderr)
            return 1
        print(f"✅ {SCHEMA_MD} is up to date")
        return 0
    SCHEMA_MD.write_text(rendered, encoding="utf-8")
    print(f"✅ wrote {SCHEMA_MD}")
    return 0


def cmd_equiv() -> int:
    declared = load_schema_from_dir()
    executed = load_schema_from_migrations()

    problems: list[str] = []

    if declared.extensions != executed.extensions:
        problems.append(
            f"extensions differ: schemas/={sorted(declared.extensions)} "
            f"migrations/={sorted(executed.extensions)}"
        )

    if declared.app_schemas != executed.app_schemas:
        problems.append(
            f"app schemas differ: schemas/={sorted(declared.app_schemas)} "
            f"migrations/={sorted(executed.app_schemas)}"
        )

    all_tables = set(declared.tables) | set(executed.tables)
    for name in sorted(all_tables):
        d = declared.tables.get(name)
        e = executed.tables.get(name)
        if d is None:
            problems.append(f"table `{name}` exists in migrations/ but not in schemas/")
            continue
        if e is None:
            problems.append(f"table `{name}` exists in schemas/ but not in migrations/")
            continue
        if d.signature() != e.signature():
            problems.append(f"table `{name}` signature differs between schemas/ and migrations/")

    all_functions = set(declared.functions) | set(executed.functions)
    for name in sorted(all_functions):
        d = declared.functions.get(name)
        e = executed.functions.get(name)
        if d is None:
            problems.append(f"function `{name}` exists in migrations/ but not in schemas/")
            continue
        if e is None:
            problems.append(f"function `{name}` exists in schemas/ but not in migrations/")
            continue
        if d.signature() != e.signature():
            problems.append(f"function `{name}` signature differs between schemas/ and migrations/")

    if problems:
        print("❌ schemas/ and migrations/ have drifted (ADR-0032 equivalence guard):", file=sys.stderr)
        for p in problems:
            print(f"  - {p}", file=sys.stderr)
        return 1

    print("✅ supabase/schemas/ matches supabase/migrations/ — no drift")
    return 0


def main() -> int:
    # This script prints ✅/❌. A Windows console defaults to cp1252, which cannot encode them, so
    # the script used to do all its work, write SCHEMA.md, and THEN die on the final print with
    # UnicodeEncodeError — reporting failure for a run that had actually succeeded. That is worse
    # than a plain crash: it trains people to distrust the guard. Callers should not have to know
    # to set PYTHONIOENCODING.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "mode",
        nargs="?",
        default="docs",
        choices=["docs", "equiv"],
        help="docs: (re)generate supabase/SCHEMA.md. equiv: check schemas/ vs migrations/ agree.",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="docs mode only: exit 1 instead of writing, if SCHEMA.md would change.",
    )
    args = parser.parse_args()

    if args.mode == "equiv":
        return cmd_equiv()
    return cmd_docs(check=args.check)


if __name__ == "__main__":
    sys.exit(main())
