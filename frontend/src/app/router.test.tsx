import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { routes } from './router.tsx'

describe('routing', () => {
  it('shows the not found page for an unknown path', () => {
    // Given
    const router = createMemoryRouter(routes, {
      initialEntries: ['/finnes-ikke'],
    })

    // When
    render(<RouterProvider router={router} />)

    // Then
    expect(
      screen.getByRole('heading', { name: 'Fant ikke siden' }),
    ).toBeInTheDocument()
  })
})
