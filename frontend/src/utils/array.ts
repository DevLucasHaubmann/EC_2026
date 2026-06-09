export function toggleArrayItem<T>(items: T[], item: T): void {
  const index = items.indexOf(item)
  if (index >= 0) {
    items.splice(index, 1)
    return
  }
  items.push(item)
}
