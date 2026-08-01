import { describe, expect, it } from 'vitest'
import { MAX_PLACE_IMAGE_BYTES, mergePlaceImages } from './mediaFiles'

const image = (name: string, type = 'image/jpeg', size = 10) =>
  new File([new Uint8Array(size)], name, { type })

describe('mergePlaceImages', () => {
  it('keeps selected images in cover order', () => {
    const first = image('cover.jpg')
    const second = image('detail.webp', 'image/webp')

    expect(mergePlaceImages([], [first, second])).toEqual({ files: [first, second], error: null })
  })

  it('rejects unsupported and oversized files', () => {
    const unsupported = image('place.gif', 'image/gif')
    const oversized = new File([new Uint8Array(1)], 'huge.png', { type: 'image/png' })
    Object.defineProperty(oversized, 'size', { value: MAX_PLACE_IMAGE_BYTES + 1 })

    expect(mergePlaceImages([], [unsupported]).files).toHaveLength(0)
    expect(mergePlaceImages([], [oversized]).files).toHaveLength(0)
  })

  it('limits a place to five images', () => {
    const current = [1, 2, 3, 4].map((number) => image(`${number}.jpg`))
    const result = mergePlaceImages(current, [image('5.jpg'), image('6.jpg')])

    expect(result.files).toHaveLength(5)
    expect(result.error).toContain('5')
  })
})
