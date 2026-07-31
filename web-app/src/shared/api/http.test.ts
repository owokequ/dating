import { describe, expect, it } from 'vitest'
import { readCookie } from './http'

describe('readCookie', () => {
  it('reads and decodes the CSRF cookie among other cookies', () => {
    expect(readCookie('XSRF-TOKEN', 'theme=dark; XSRF-TOKEN=a%2Fb%3D; session=x')).toBe('a/b=')
  })

  it('does not match similarly named cookies', () => {
    expect(readCookie('XSRF-TOKEN', 'OLD-XSRF-TOKEN=value')).toBeNull()
  })
})
