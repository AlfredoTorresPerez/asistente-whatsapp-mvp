interface FacebookLoginResponse {
  status?: string
  authResponse?: {
    code?: string
    accessToken?: string
    waba_id?: string
    phone_number_id?: string
  }
}

interface FacebookSDK {
  init(params: { appId: string; version: string; cookie?: boolean; xfbml?: boolean }): void
  login(
    callback: (response: FacebookLoginResponse) => void,
    options?: Record<string, unknown>,
  ): void
  Event: {
    subscribe(event: string, callback: (response: FacebookLoginResponse) => void): () => void
  }
}

interface Window {
  FB: FacebookSDK
}
