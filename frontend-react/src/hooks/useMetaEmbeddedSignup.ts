import { useCallback, useState } from 'react'
import { metaEmbeddedSignup, type MetaEmbeddedSignupResult } from '../services/MetaEmbeddedSignup'

export function useMetaEmbeddedSignup() {
  const [isOpening, setIsOpening] = useState(false)
  const [error, setError] = useState<Error | null>(null)
  const [result, setResult] = useState<MetaEmbeddedSignupResult | null>(null)

  const appId = import.meta.env.VITE_META_APP_ID
  const configId = import.meta.env.VITE_META_EMBEDDED_SIGNUP_CONFIG_ID

  const openSignup = useCallback(async (): Promise<MetaEmbeddedSignupResult> => {
    setIsOpening(true)
    setError(null)
    setResult(null)

    try {
      const signupResult = await metaEmbeddedSignup.openSignupDialog({
        appId,
        configId,
      })
      setResult(signupResult)
      return signupResult
    } catch (err) {
      const errorInstance = err instanceof Error ? err : new Error(String(err))
      setError(errorInstance)
      throw errorInstance
    } finally {
      setIsOpening(false)
    }
  }, [appId, configId])

  return { openSignup, isOpening, error, result }
}
