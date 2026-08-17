import "@testing-library/jest-dom/vitest";

// jsdom doesn't implement SVG geometry APIs (real browsers do) — AreaChart's stroke draw-in
// animation calls this on mount, so any SVG-path component would otherwise crash in tests only.
// Patched on SVGElement, not SVGPathElement: this jsdom version has no distinct SVGPathElement
// constructor at all — <path> nodes are plain SVGElement instances (verified empirically; a
// SVGPathElement-targeted patch is a silent no-op since that global doesn't exist here).
if (typeof globalThis.SVGElement !== "undefined") {
  // lib.dom.d.ts declares getTotalLength on SVGGeometryElement, not the base SVGElement — but
  // jsdom's own runtime object graph (unlike a real browser's) has no SVGGeometryElement/
  // SVGPathElement split, so the patch target has to be the base class the type defs disagree
  // with. Cast to the shape jsdom actually exposes rather than fighting the lib types.
  (globalThis.SVGElement.prototype as unknown as { getTotalLength: () => number }).getTotalLength =
    () => 0;
}
