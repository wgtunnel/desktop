package util

import (
	"fmt"
	"math"
)

// GenerateHandle generates a unique int32 handle for a given map.
func GenerateHandle[K int32, V any](handles map[K]V) (int32, error) {
	for i := int32(0); i < math.MaxInt32; i++ {
		if _, exists := handles[K(i)]; !exists {
			return i, nil
		}
	}
	return -1, fmt.Errorf("unable to find handle")
}
