export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  // Allows to automatically instantiate createClient with right options
  // instead of createClient<Database, { PostgrestVersion: 'XX' }>(URL, KEY)
  __InternalSupabase: {
    PostgrestVersion: "14.5"
  }
  finance: {
    Tables: {
      accounts: {
        Row: {
          created_at: string
          deleted_at: string | null
          due_day: number | null
          id: string
          is_primary: boolean
          limit_paise: number | null
          mask: string | null
          name: string
          opening_balance_paise: number
          reconciled_at: string | null
          request_id: string | null
          type: string
          user_id: string
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          due_day?: number | null
          id?: string
          is_primary?: boolean
          limit_paise?: number | null
          mask?: string | null
          name: string
          opening_balance_paise?: number
          reconciled_at?: string | null
          request_id?: string | null
          type: string
          user_id: string
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          due_day?: number | null
          id?: string
          is_primary?: boolean
          limit_paise?: number | null
          mask?: string | null
          name?: string
          opening_balance_paise?: number
          reconciled_at?: string | null
          request_id?: string | null
          type?: string
          user_id?: string
        }
        Relationships: []
      }
      categories: {
        Row: {
          created_at: string
          deleted_at: string | null
          excluded_from_spend: boolean
          icon: string | null
          id: string
          kind: string
          name: string
          parent_id: string | null
          request_id: string | null
          user_id: string
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          excluded_from_spend?: boolean
          icon?: string | null
          id?: string
          kind: string
          name: string
          parent_id?: string | null
          request_id?: string | null
          user_id: string
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          excluded_from_spend?: boolean
          icon?: string | null
          id?: string
          kind?: string
          name?: string
          parent_id?: string | null
          request_id?: string | null
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "categories_parent_id_fkey"
            columns: ["parent_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id"]
          },
        ]
      }
      holdings: {
        Row: {
          created_at: string
          deleted_at: string | null
          id: string
          invested_paise: number | null
          kind: string
          name: string
          notes: string | null
          request_id: string | null
          sector: string
          user_id: string
        }
        Insert: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          invested_paise?: number | null
          kind: string
          name: string
          notes?: string | null
          request_id?: string | null
          sector: string
          user_id: string
        }
        Update: {
          created_at?: string
          deleted_at?: string | null
          id?: string
          invested_paise?: number | null
          kind?: string
          name?: string
          notes?: string | null
          request_id?: string | null
          sector?: string
          user_id?: string
        }
        Relationships: []
      }
      liabilities_meta: {
        Row: {
          collateral: string | null
          created_at: string
          debit_day: number | null
          deleted_at: string | null
          emi_paise: number | null
          holding_id: string
          liability_type: string
          linked_account_id: string | null
          original_principal_paise: number | null
          paid_months: number
          rate_bps: number
          request_id: string | null
          tenure_months: number | null
          updated_at: string
        }
        Insert: {
          collateral?: string | null
          created_at?: string
          debit_day?: number | null
          deleted_at?: string | null
          emi_paise?: number | null
          holding_id: string
          liability_type: string
          linked_account_id?: string | null
          original_principal_paise?: number | null
          paid_months?: number
          rate_bps: number
          request_id?: string | null
          tenure_months?: number | null
          updated_at?: string
        }
        Update: {
          collateral?: string | null
          created_at?: string
          debit_day?: number | null
          deleted_at?: string | null
          emi_paise?: number | null
          holding_id?: string
          liability_type?: string
          linked_account_id?: string | null
          original_principal_paise?: number | null
          paid_months?: number
          rate_bps?: number
          request_id?: string | null
          tenure_months?: number | null
          updated_at?: string
        }
        Relationships: [
          {
            foreignKeyName: "liabilities_meta_holding_id_fkey"
            columns: ["holding_id"]
            isOneToOne: true
            referencedRelation: "holdings"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "liabilities_meta_linked_account_id_fkey"
            columns: ["linked_account_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "liabilities_meta_linked_account_id_fkey"
            columns: ["linked_account_id"]
            isOneToOne: false
            referencedRelation: "v_account_balances"
            referencedColumns: ["account_id"]
          },
        ]
      }
      recurring_templates: {
        Row: {
          amount_is_variable: boolean
          created_at: string
          deleted_at: string | null
          id: string
          next_run: string
          paused: boolean
          paused_at: string | null
          request_id: string | null
          rrule: string
          template: Json
          user_id: string
        }
        Insert: {
          amount_is_variable?: boolean
          created_at?: string
          deleted_at?: string | null
          id?: string
          next_run: string
          paused?: boolean
          paused_at?: string | null
          request_id?: string | null
          rrule: string
          template: Json
          user_id: string
        }
        Update: {
          amount_is_variable?: boolean
          created_at?: string
          deleted_at?: string | null
          id?: string
          next_run?: string
          paused?: boolean
          paused_at?: string | null
          request_id?: string | null
          rrule?: string
          template?: Json
          user_id?: string
        }
        Relationships: []
      }
      suggestions: {
        Row: {
          created_at: string
          due_on: string | null
          id: string
          parsed: Json
          raw_text: string | null
          recurring_id: string | null
          status: string
          user_id: string
        }
        Insert: {
          created_at?: string
          due_on?: string | null
          id?: string
          parsed: Json
          raw_text?: string | null
          recurring_id?: string | null
          status?: string
          user_id: string
        }
        Update: {
          created_at?: string
          due_on?: string | null
          id?: string
          parsed?: Json
          raw_text?: string | null
          recurring_id?: string | null
          status?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "suggestions_recurring_id_fkey"
            columns: ["recurring_id"]
            isOneToOne: false
            referencedRelation: "recurring_templates"
            referencedColumns: ["id"]
          },
        ]
      }
      transaction_events: {
        Row: {
          at: string
          detail: Json | null
          id: string
          kind: string
          transaction_id: string
        }
        Insert: {
          at?: string
          detail?: Json | null
          id?: string
          kind: string
          transaction_id: string
        }
        Update: {
          at?: string
          detail?: Json | null
          id?: string
          kind?: string
          transaction_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "transaction_events_transaction_id_fkey"
            columns: ["transaction_id"]
            isOneToOne: false
            referencedRelation: "transactions"
            referencedColumns: ["id"]
          },
        ]
      }
      transactions: {
        Row: {
          account_id: string
          amount_paise: number
          category_id: string | null
          cleared: boolean
          created_at: string
          deleted_at: string | null
          goal_id: string | null
          id: string
          note: string | null
          occurred_at: string
          payee: string | null
          receipt_path: string | null
          recurring_id: string | null
          request_id: string | null
          source: string
          split_group_id: string | null
          to_account_id: string | null
          type: string
          user_id: string
        }
        Insert: {
          account_id: string
          amount_paise: number
          category_id?: string | null
          cleared?: boolean
          created_at?: string
          deleted_at?: string | null
          goal_id?: string | null
          id?: string
          note?: string | null
          occurred_at: string
          payee?: string | null
          receipt_path?: string | null
          recurring_id?: string | null
          request_id?: string | null
          source?: string
          split_group_id?: string | null
          to_account_id?: string | null
          type: string
          user_id: string
        }
        Update: {
          account_id?: string
          amount_paise?: number
          category_id?: string | null
          cleared?: boolean
          created_at?: string
          deleted_at?: string | null
          goal_id?: string | null
          id?: string
          note?: string | null
          occurred_at?: string
          payee?: string | null
          receipt_path?: string | null
          recurring_id?: string | null
          request_id?: string | null
          source?: string
          split_group_id?: string | null
          to_account_id?: string | null
          type?: string
          user_id?: string
        }
        Relationships: [
          {
            foreignKeyName: "transactions_account_id_fkey"
            columns: ["account_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "transactions_account_id_fkey"
            columns: ["account_id"]
            isOneToOne: false
            referencedRelation: "v_account_balances"
            referencedColumns: ["account_id"]
          },
          {
            foreignKeyName: "transactions_category_id_fkey"
            columns: ["category_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "transactions_recurring_id_fkey"
            columns: ["recurring_id"]
            isOneToOne: false
            referencedRelation: "recurring_templates"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "transactions_to_account_id_fkey"
            columns: ["to_account_id"]
            isOneToOne: false
            referencedRelation: "accounts"
            referencedColumns: ["id"]
          },
          {
            foreignKeyName: "transactions_to_account_id_fkey"
            columns: ["to_account_id"]
            isOneToOne: false
            referencedRelation: "v_account_balances"
            referencedColumns: ["account_id"]
          },
        ]
      }
      valuations: {
        Row: {
          as_of: string
          created_at: string
          deleted_at: string | null
          holding_id: string
          id: string
          request_id: string | null
          source: string
          value_paise: number
        }
        Insert: {
          as_of: string
          created_at?: string
          deleted_at?: string | null
          holding_id: string
          id?: string
          request_id?: string | null
          source: string
          value_paise: number
        }
        Update: {
          as_of?: string
          created_at?: string
          deleted_at?: string | null
          holding_id?: string
          id?: string
          request_id?: string | null
          source?: string
          value_paise?: number
        }
        Relationships: [
          {
            foreignKeyName: "valuations_holding_id_fkey"
            columns: ["holding_id"]
            isOneToOne: false
            referencedRelation: "holdings"
            referencedColumns: ["id"]
          },
        ]
      }
    }
    Views: {
      v_account_balances: {
        Row: {
          account_id: string | null
          balance_paise: number | null
          counts_as_spendable: boolean | null
          type: string | null
          user_id: string | null
        }
        Relationships: []
      }
      v_category_spend: {
        Row: {
          category_id: string | null
          category_kind: string | null
          category_name: string | null
          excluded_from_spend: boolean | null
          month: string | null
          share_percent_tenths: number | null
          spend_paise: number | null
          user_id: string | null
        }
        Relationships: [
          {
            foreignKeyName: "transactions_category_id_fkey"
            columns: ["category_id"]
            isOneToOne: false
            referencedRelation: "categories"
            referencedColumns: ["id"]
          },
        ]
      }
      v_latest_valuation: {
        Row: {
          as_of: string | null
          created_at: string | null
          holding_id: string | null
          source: string | null
          user_id: string | null
          valuation_id: string | null
          value_paise: number | null
        }
        Relationships: [
          {
            foreignKeyName: "valuations_holding_id_fkey"
            columns: ["holding_id"]
            isOneToOne: false
            referencedRelation: "holdings"
            referencedColumns: ["id"]
          },
        ]
      }
      v_month_summary: {
        Row: {
          excluded_paise: number | null
          expense_paise: number | null
          income_paise: number | null
          month: string | null
          transfer_paise: number | null
          user_id: string | null
        }
        Relationships: []
      }
      v_net_worth_by_sector: {
        Row: {
          holding_count: number | null
          kind: string | null
          sector: string | null
          user_id: string | null
          value_paise: number | null
        }
        Relationships: []
      }
      v_net_worth_history: {
        Row: {
          as_of: string | null
          assets_paise: number | null
          liabilities_paise: number | null
          net_paise: number | null
          user_id: string | null
        }
        Relationships: []
      }
    }
    Functions: {
      correct_valuation: {
        Args: {
          p_as_of: string
          p_note?: string
          p_valuation_id: string
          p_value_paise: number
        }
        Returns: string
      }
      create_holding_with_value: {
        Args: {
          p_as_of: string
          p_invested_paise?: number
          p_kind: string
          p_name: string
          p_notes?: string
          p_request_id?: string
          p_sector: string
          p_source?: string
          p_value_paise: number
        }
        Returns: string
      }
      merge_categories: {
        Args: { p_source: string; p_target: string }
        Returns: number
      }
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
  public: {
    Tables: {
      [_ in never]: never
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      delete_my_account: { Args: never; Returns: undefined }
      delete_my_data: { Args: never; Returns: undefined }
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}

type DatabaseWithoutInternals = Omit<Database, "__InternalSupabase">

type DefaultSchema = DatabaseWithoutInternals[Extract<keyof Database, "public">]

export type Tables<
  DefaultSchemaTableNameOrOptions extends
    | keyof (DefaultSchema["Tables"] & DefaultSchema["Views"])
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
        DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? (DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"] &
      DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Views"])[TableName] extends {
      Row: infer R
    }
    ? R
    : never
  : DefaultSchemaTableNameOrOptions extends keyof (DefaultSchema["Tables"] &
        DefaultSchema["Views"])
    ? (DefaultSchema["Tables"] &
        DefaultSchema["Views"])[DefaultSchemaTableNameOrOptions] extends {
        Row: infer R
      }
      ? R
      : never
    : never

export type TablesInsert<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Insert: infer I
    }
    ? I
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Insert: infer I
      }
      ? I
      : never
    : never

export type TablesUpdate<
  DefaultSchemaTableNameOrOptions extends
    | keyof DefaultSchema["Tables"]
    | { schema: keyof DatabaseWithoutInternals },
  TableName extends (DefaultSchemaTableNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"]
    : never) = never,
> = DefaultSchemaTableNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaTableNameOrOptions["schema"]]["Tables"][TableName] extends {
      Update: infer U
    }
    ? U
    : never
  : DefaultSchemaTableNameOrOptions extends keyof DefaultSchema["Tables"]
    ? DefaultSchema["Tables"][DefaultSchemaTableNameOrOptions] extends {
        Update: infer U
      }
      ? U
      : never
    : never

export type Enums<
  DefaultSchemaEnumNameOrOptions extends
    | keyof DefaultSchema["Enums"]
    | { schema: keyof DatabaseWithoutInternals },
  EnumName extends (DefaultSchemaEnumNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"]
    : never) = never,
> = DefaultSchemaEnumNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[DefaultSchemaEnumNameOrOptions["schema"]]["Enums"][EnumName]
  : DefaultSchemaEnumNameOrOptions extends keyof DefaultSchema["Enums"]
    ? DefaultSchema["Enums"][DefaultSchemaEnumNameOrOptions]
    : never

export type CompositeTypes<
  PublicCompositeTypeNameOrOptions extends
    | keyof DefaultSchema["CompositeTypes"]
    | { schema: keyof DatabaseWithoutInternals },
  CompositeTypeName extends (PublicCompositeTypeNameOrOptions extends {
    schema: keyof DatabaseWithoutInternals
  }
    ? keyof DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"]
    : never) = never,
> = PublicCompositeTypeNameOrOptions extends {
  schema: keyof DatabaseWithoutInternals
}
  ? DatabaseWithoutInternals[PublicCompositeTypeNameOrOptions["schema"]]["CompositeTypes"][CompositeTypeName]
  : PublicCompositeTypeNameOrOptions extends keyof DefaultSchema["CompositeTypes"]
    ? DefaultSchema["CompositeTypes"][PublicCompositeTypeNameOrOptions]
    : never

export const Constants = {
  finance: {
    Enums: {},
  },
  public: {
    Enums: {},
  },
} as const
