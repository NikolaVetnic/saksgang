import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <>
      <h1>Fant ikke siden</h1>
      <p>
        <Link to="/">Gå til forsiden</Link>
      </p>
    </>
  )
}
