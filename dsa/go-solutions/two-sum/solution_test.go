package twosum

import (
	"reflect"
	"testing"
)

func TestTwoSum(t *testing.T) {
	cases := []struct {
		name     string
		nums     []int
		target   int
		expected []int
	}{
		{"basic pair", []int{2, 7, 11, 15}, 9, []int{0, 1}},
		{"duplicate values", []int{3, 3}, 6, []int{0, 1}},
		{"negative numbers", []int{-1, -2, -3}, -5, []int{1, 2}},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			got := TwoSum(tc.nums, tc.target)
			if !reflect.DeepEqual(got, tc.expected) {
				t.Errorf("TwoSum(%v, %d) = %v; want %v", tc.nums, tc.target, got, tc.expected)
			}
		})
	}
}
