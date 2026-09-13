package main

/*
#include <jni.h>
#include <stdlib.h>

static const char* jstring_to_c(JNIEnv* env, jstring str) {
	if (str == NULL) return NULL;
	return (*env)->GetStringUTFChars(env, str, NULL);
}

static void release_jstring(JNIEnv* env, jstring str, const char* chars) {
	if (str != NULL && chars != NULL) {
		(*env)->ReleaseStringUTFChars(env, str, chars);
	}
}

static jstring c_to_jstring(JNIEnv* env, const char* chars) {
	if (chars == NULL) return NULL;
	return (*env)->NewStringUTF(env, chars);
}

static jstring null_jstring(void) {
	return NULL;
}
*/
import "C"

import (
	"errors"
	"unsafe"

	"github.com/zalando/go-keyring"
)

//export Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_storeSecret
func Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_storeSecret(
	env *C.JNIEnv,
	_ C.jclass,
	jService C.jstring,
	jName C.jstring,
	jValue C.jstring,
) C.jint {
	service := C.jstring_to_c(env, jService)
	name := C.jstring_to_c(env, jName)
	value := C.jstring_to_c(env, jValue)
	defer C.release_jstring(env, jService, service)
	defer C.release_jstring(env, jName, name)
	defer C.release_jstring(env, jValue, value)

	if service == nil || name == nil || value == nil {
		return C.jint(-1)
	}

	err := keyring.Set(C.GoString(service), C.GoString(name), C.GoString(value))
	if err != nil {
		return C.jint(-1)
	}
	return C.jint(1)
}

//export Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_getSecret
func Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_getSecret(
	env *C.JNIEnv,
	_ C.jclass,
	jService C.jstring,
	jName C.jstring,
) C.jstring {
	service := C.jstring_to_c(env, jService)
	name := C.jstring_to_c(env, jName)
	defer C.release_jstring(env, jService, service)
	defer C.release_jstring(env, jName, name)

	if service == nil || name == nil {
		return C.null_jstring()
	}

	value, err := keyring.Get(C.GoString(service), C.GoString(name))
	if err != nil {
		return C.null_jstring()
	}

	cValue := C.CString(value)
	defer C.free(unsafe.Pointer(cValue))
	return C.c_to_jstring(env, cValue)
}

//export Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_deleteSecret
func Java_com_zaneschepke_wireguardautotunnel_keyring_KeyringNative_deleteSecret(
	env *C.JNIEnv,
	_ C.jclass,
	jService C.jstring,
	jName C.jstring,
) C.jint {
	service := C.jstring_to_c(env, jService)
	name := C.jstring_to_c(env, jName)
	defer C.release_jstring(env, jService, service)
	defer C.release_jstring(env, jName, name)

	if service == nil || name == nil {
		return C.jint(-1)
	}

	err := keyring.Delete(C.GoString(service), C.GoString(name))
	if err != nil {
		if errors.Is(err, keyring.ErrNotFound) {
			return C.jint(-1)
		}
		return C.jint(-1)
	}
	return C.jint(1)
}

func main() {}
