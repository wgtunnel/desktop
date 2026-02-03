package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"errors"

	"github.com/zalando/go-keyring"
)

//export storeSecret
func storeSecret(service *C.char, name *C.char, value *C.char) C.int {
	if service == nil || name == nil || value == nil {
		return C.int(-1)
	}

	err := keyring.Set(
		C.GoString(service),
		C.GoString(name),
		C.GoString(value),
	)

	if err != nil {
		return C.int(-1)
	}

	return C.int(1)
}

//export getSecret
func getSecret(service *C.char, name *C.char) *C.char {
	if service == nil || name == nil {
		return nil
	}

	value, err := keyring.Get(
		C.GoString(service),
		C.GoString(name),
	)

	if err != nil {
		if errors.Is(err, keyring.ErrNotFound) {
			return nil
		}
		return nil
	}

	return C.CString(value)
}

//export deleteSecret
func deleteSecret(service *C.char, name *C.char) C.int {
	if service == nil || name == nil {
		return C.int(-1)
	}

	err := keyring.Delete(
		C.GoString(service),
		C.GoString(name),
	)

	if err != nil {
		if errors.Is(err, keyring.ErrNotFound) {
			return C.int(-1)
		}
		return C.int(-1)
	}

	return C.int(1)
}

func main() {}
