import { describe, test, expect } from "vitest";
import { twoSum } from "./solution.js";

describe("twoSum", () => {
  test("finds a basic pair", () => {
    expect(twoSum([2, 7, 11, 15], 9)).toEqual([0, 1]);
  });

  test("handles duplicate values", () => {
    expect(twoSum([3, 3], 6)).toEqual([0, 1]);
  });

  test("handles negative numbers", () => {
    expect(twoSum([-1, -2, -3], -5)).toEqual([1, 2]);
  });

  test("returns an empty array when no pair exists", () => {
    expect(twoSum([1, 2, 3], 100)).toEqual([]);
  });
});
