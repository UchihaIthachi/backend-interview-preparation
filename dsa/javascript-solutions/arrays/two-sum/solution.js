export function twoSum(numbers, target) {
  const indexes = new Map();

  for (let index = 0; index < numbers.length; index++) {
    const required = target - numbers[index];

    if (indexes.has(required)) {
      return [indexes.get(required), index];
    }

    indexes.set(numbers[index], index);
  }

  return [];
}
