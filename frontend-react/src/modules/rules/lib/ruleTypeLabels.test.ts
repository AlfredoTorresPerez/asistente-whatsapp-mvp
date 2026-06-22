import { describe, expect, it } from 'vitest'
import { formatRuleType } from './ruleTypeLabels'

describe('formatRuleType', () => {
  it('translates required technical rule codes to Spanish labels', () => {
    expect(formatRuleType('ALLOWED')).toBe('Permitido')
    expect(formatRuleType('BLOCKED')).toBe('Bloqueado')
    expect(formatRuleType('ESCALATION')).toBe('Derivación humana')
    expect(formatRuleType('SCHEDULE')).toBe('Horario')
    expect(formatRuleType('PRICE')).toBe('Precio')
    expect(formatRuleType('POLICY')).toBe('Política')
    expect(formatRuleType('FAQ')).toBe('Pregunta frecuente')
    expect(formatRuleType('INTENT')).toBe('Intención')
    expect(formatRuleType('KNOWLEDGE')).toBe('Base de conocimiento')
  })

  it('keeps unknown values readable without breaking the backend contract', () => {
    expect(formatRuleType('CUSTOM_RULE_TYPE')).toBe('Custom Rule Type')
    expect(formatRuleType('')).toBe('Sin clasificar')
  })
})
