package main

import (
	"syscall"
	"unsafe"
)

const (
	processSetInformation             = 0x0200
	processQueryInformation           = 0x0400
	processPowerThrottling            = 4
	processPowerThrottlingVersion     = 1
	processPowerThrottlingExecSpeed   = 0x1
	processPowerThrottlingIgnoreTimer = 0x4
	aboveNormalPriorityClass          = 0x00008000
)

type processPowerThrottlingState struct {
	Version     uint32
	ControlMask uint32
	StateMask   uint32
}

var (
	procSetPriorityClass      = kernel32.NewProc("SetPriorityClass")
	procSetProcessInformation = kernel32.NewProc("SetProcessInformation")
)

func pinChildToForegroundSpeed(pid int) {
	handle, err := syscall.OpenProcess(processSetInformation|processQueryInformation, false, uint32(pid))
	if err != nil {
		return
	}
	defer syscall.CloseHandle(handle)

	procSetPriorityClass.Call(uintptr(handle), aboveNormalPriorityClass)

	state := processPowerThrottlingState{
		Version:     processPowerThrottlingVersion,
		ControlMask: processPowerThrottlingExecSpeed | processPowerThrottlingIgnoreTimer,
		StateMask:   0,
	}
	if !setPowerThrottling(handle, state) {
		state.ControlMask = processPowerThrottlingExecSpeed
		setPowerThrottling(handle, state)
	}
}

func setPowerThrottling(handle syscall.Handle, state processPowerThrottlingState) bool {
	r1, _, _ := procSetProcessInformation.Call(
		uintptr(handle),
		processPowerThrottling,
		uintptr(unsafe.Pointer(&state)),
		unsafe.Sizeof(state),
	)
	return r1 != 0
}
