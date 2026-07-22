export type MetaEmbeddedSignupConfig = {
  appId: string
  configId: string
  businessId?: string
}

export type MetaEmbeddedSignupResult = {
  code: string
  wabaId?: string
  phoneNumberId?: string
}

const FB_SDK_URL = 'https://connect.facebook.net/en_US/sdk.js'
const SDK_LOAD_TIMEOUT_MS = 15_000
const SIGNUP_TIMEOUT_MS = 300_000
const DEFAULT_GRAPH_API_VERSION = 'v23.0'
const FB_ORIGIN = 'https://www.facebook.com'

function isFacebookSdkLoaded(): boolean {
  return typeof window.FB !== 'undefined' && typeof window.FB.init === 'function'
}

type FacebookLoginResponse = {
  status?: string
  authResponse?: {
    code?: string
    accessToken?: string
    waba_id?: string
    phone_number_id?: string
  }
}

type WhatsappEmbeddedSignupEvent = {
  type?: string
  waba_id?: string
  phone_number_id?: string
  status?: string
}

export class MetaEmbeddedSignup {
  private sdkLoadPromise: Promise<true> | null = null
  private sdkLoaded = false
  private messageHandler: ((event: MessageEvent) => void) | null = null

  private loadSdk(): Promise<true> {
    if (this.sdkLoaded && isFacebookSdkLoaded()) {
      return Promise.resolve(true)
    }

    if (this.sdkLoadPromise) {
      return this.sdkLoadPromise
    }

    this.sdkLoadPromise = new Promise<true>((resolve, reject) => {
      if (isFacebookSdkLoaded()) {
        this.sdkLoaded = true
        resolve(true)
        return
      }

      const script = document.createElement('script')
      script.src = FB_SDK_URL
      script.async = true
      script.defer = true
      script.crossOrigin = 'anonymous'

      const timeoutId = window.setTimeout(() => {
        cleanup()
        reject(new Error('Facebook SDK load timed out after 15 seconds'))
      }, SDK_LOAD_TIMEOUT_MS)

      const cleanup = () => {
        window.clearTimeout(timeoutId)
        script.removeEventListener('load', onLoad)
        script.removeEventListener('error', onError)
      }

      const onLoad = () => {
        cleanup()
        this.sdkLoaded = true
        resolve(true)
      }

      const onError = () => {
        cleanup()
        reject(new Error('Failed to load Facebook SDK'))
      }

      script.addEventListener('load', onLoad)
      script.addEventListener('error', onError)
      document.body.appendChild(script)
    })

    return this.sdkLoadPromise
  }

  private cleanupMessageHandler(): void {
    if (this.messageHandler) {
      window.removeEventListener('message', this.messageHandler)
      this.messageHandler = null
    }
  }

  async openSignupDialog(config: MetaEmbeddedSignupConfig): Promise<MetaEmbeddedSignupResult> {
    const appId = config.appId
    const configId = config.configId
    const graphApiVersion = import.meta.env.VITE_META_GRAPH_API_VERSION ?? DEFAULT_GRAPH_API_VERSION

    if (!appId) {
      throw new Error('VITE_META_APP_ID no esta configurado')
    }
    if (!configId) {
      throw new Error('VITE_META_EMBEDDED_SIGNUP_CONFIG_ID no esta configurado')
    }

    await this.loadSdk()

    window.FB.init({
      appId,
      version: graphApiVersion,
    })

    return new Promise<MetaEmbeddedSignupResult>((resolve, reject) => {
      let resolved = false
      let timeoutId: ReturnType<typeof setTimeout> | null = null
      let cancelUnsubscribe: () => void = () => {}

      const cleanup = () => {
        this.cleanupMessageHandler()
        if (timeoutId !== null) {
          clearTimeout(timeoutId)
          timeoutId = null
        }
        if (cancelUnsubscribe) {
          try {
            cancelUnsubscribe()
          } catch {
            // ignore
          }
        }
      }

      timeoutId = setTimeout(() => {
        cleanup()
        reject(new Error('Signup timed out after 5 minutes'))
      }, SIGNUP_TIMEOUT_MS)

      // Listen for WhatsApp Embedded Signup events via window message
      this.messageHandler = (event: MessageEvent) => {
        if (event.origin !== FB_ORIGIN && event.origin !== 'https://business.facebook.com') {
          return
        }

        try {
          const data: WhatsappEmbeddedSignupEvent =
            typeof event.data === 'object' ? event.data : JSON.parse(event.data)

          if (data.type === 'WA_EMBEDDED_SIGNUP' || data.type === 'whatsapp_signup') {
            if (data.status === 'success' && data.waba_id && data.phone_number_id) {
              if (!resolved) {
                resolved = true
                cleanup()
                resolve({
                  code: '',
                  wabaId: data.waba_id,
                  phoneNumberId: data.phone_number_id,
                })
              }
            } else if (data.status === 'cancel' || data.status === 'error') {
              if (!resolved) {
                resolved = true
                cleanup()
                reject(
                  new Error(data.status === 'cancel' ? 'Signup was cancelled' : 'Signup error'),
                )
              }
            }
          }
        } catch {
          // ignore non-JSON messages
        }
      }
      window.addEventListener('message', this.messageHandler)

      cancelUnsubscribe = window.FB.Event.subscribe(
        'auth.statusChange',
        (response: FacebookLoginResponse) => {
          if (resolved) return
          if (response.status === 'not_authorized' || response.status === 'unknown') {
            resolved = true
            cleanup()
            reject(new Error('Facebook signup was cancelled by the user'))
          }
        },
      )

      window.FB.login(
        (response: FacebookLoginResponse) => {
          if (resolved) return

          if (!response || response.status === 'not_authorized' || response.status === 'unknown') {
            resolved = true
            cleanup()
            reject(new Error('Facebook signup was cancelled or not authorized'))
            return
          }

          const authResponse = response.authResponse

          if (!authResponse || !authResponse.code) {
            resolved = true
            cleanup()
            reject(new Error('Facebook signup failed: no authorization code received'))
            return
          }

          // If waba_id and phone_number_id came from FB.login, resolve immediately
          if (authResponse.waba_id || authResponse.phone_number_id) {
            resolved = true
            cleanup()
            resolve({
              code: authResponse.code,
              wabaId: authResponse.waba_id,
              phoneNumberId: authResponse.phone_number_id,
            })
            return
          }

          // Otherwise wait for window message event
          // The code will be sent separately via the window message listener
        },
        {
          config_id: configId,
          response_type: 'code',
          override_default_response_type: true,
        },
      )
    })
  }
}

export const metaEmbeddedSignup = new MetaEmbeddedSignup()
