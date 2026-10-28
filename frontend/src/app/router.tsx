import { createBrowserRouter } from 'react-router'
import { HomePage } from './HomePage.tsx'
import { Layout } from './Layout.tsx'
import { NotFoundPage } from './NotFoundPage.tsx'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
