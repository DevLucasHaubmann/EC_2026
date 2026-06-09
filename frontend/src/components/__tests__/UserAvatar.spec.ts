// @vitest-environment jsdom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import UserAvatar from '@/components/UserAvatar.vue'

// ── UserAvatar ─────────────────────────────────────────────────────────────────

describe('UserAvatar — initials fallback', () => {
  it('renders initials when no avatarUrl is provided', () => {
    const wrapper = mount(UserAvatar, { props: { name: 'Lucas Haubmann' } })
    expect(wrapper.find('.user-avatar--initials').exists()).toBe(true)
    expect(wrapper.find('.user-avatar--initials').text()).toBe('LH')
  })

  it('renders initials when avatarUrl is null', () => {
    const wrapper = mount(UserAvatar, { props: { name: 'Ana Clara', avatarUrl: null } })
    expect(wrapper.find('.user-avatar--initials').exists()).toBe(true)
    expect(wrapper.find('.user-avatar--initials').text()).toBe('AC')
  })

  it('falls back to email initial when name is not provided', () => {
    const wrapper = mount(UserAvatar, {
      props: { email: 'maria@tukan.test', avatarUrl: null },
    })
    expect(wrapper.find('.user-avatar--initials').text()).toBe('M')
  })

  it('shows "?" when neither name nor email is provided', () => {
    const wrapper = mount(UserAvatar, { props: { avatarUrl: null } })
    expect(wrapper.find('.user-avatar--initials').text()).toBe('?')
  })
})

describe('UserAvatar — image rendering', () => {
  it('renders an <img> when a valid https URL is provided', () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'Lucas',
        avatarUrl: 'https://example.com/avatar.png',
      },
    })
    expect(wrapper.find('img').exists()).toBe(true)
    expect(wrapper.find('img').attributes('src')).toBe('https://example.com/avatar.png')
  })

  it('does not render an <img> when the URL uses a non-http(s) protocol', () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'Lucas',
        avatarUrl: 'javascript:alert(1)',
      },
    })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.user-avatar--initials').exists()).toBe(true)
  })

  it('does not render an <img> for a data: URL', () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'Lucas',
        avatarUrl: 'data:image/png;base64,abc',
      },
    })
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('falls back to initials after image load error', async () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'Lucas',
        avatarUrl: 'https://example.com/missing.png',
      },
    })
    // Image is rendered initially
    expect(wrapper.find('img').exists()).toBe(true)

    // Simulate the browser firing an error event on the img element
    await wrapper.find('img').trigger('error')

    // After error the initials fallback takes over
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.find('.user-avatar--initials').exists()).toBe(true)
    expect(wrapper.find('.user-avatar--initials').text()).toBe('L')
  })

  it('recovers and renders the image when avatarUrl changes after an error', async () => {
    const wrapper = mount(UserAvatar, {
      props: {
        name: 'Lucas',
        avatarUrl: 'https://example.com/missing.png',
      },
    })
    await wrapper.find('img').trigger('error')
    expect(wrapper.find('img').exists()).toBe(false)

    // A new valid URL must clear the stale error state and render the image again
    await wrapper.setProps({ avatarUrl: 'https://example.com/new-avatar.png' })

    expect(wrapper.find('img').exists()).toBe(true)
    expect(wrapper.find('img').attributes('src')).toBe('https://example.com/new-avatar.png')
  })
})

describe('UserAvatar — size prop', () => {
  it('applies the default size of 40 when size prop is omitted', () => {
    const wrapper = mount(UserAvatar, { props: { name: 'Test' } })
    const root = wrapper.element as HTMLElement
    expect(root.style.width).toBe('40px')
    expect(root.style.height).toBe('40px')
  })

  it('applies a custom size via inline style', () => {
    const wrapper = mount(UserAvatar, { props: { name: 'Test', size: 72 } })
    const root = wrapper.element as HTMLElement
    expect(root.style.width).toBe('72px')
    expect(root.style.height).toBe('72px')
  })
})
