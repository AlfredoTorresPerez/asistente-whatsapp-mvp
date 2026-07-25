import { useQuery } from '@tanstack/react-query'
import { getPublicContentItemsRequest } from '../../../services/api/contentApi'
import type { PublicContentItemResponse } from '../../../services/api/types'

export function usePublicContent(type?: string) {
  return useQuery({
    queryKey: ['public-content', type],
    queryFn: () => getPublicContentItemsRequest(type),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

export function usePublicLandingContent() {
  const { data, isLoading, error, refetch } = usePublicContent('LANDING_PAGE')
  
  return {
    items: data ?? [],
    isLoading,
    error,
    refetch,
  }
}

export function usePublicServicesContent() {
  const { data, isLoading, error, refetch } = usePublicContent('SERVICE')
  
  return {
    items: data ?? [],
    isLoading,
    error,
    refetch,
  }
}

export function usePublicCategoriesContent() {
  const { data, isLoading, error, refetch } = usePublicContent('CATEGORY')
  
  return {
    items: data ?? [],
    isLoading,
    error,
    refetch,
  }
}