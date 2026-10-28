import { Link, Outlet } from 'react-router'

export function Layout() {
  return (
    <>
      <header>
        <Link to="/">Saksgang</Link>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
