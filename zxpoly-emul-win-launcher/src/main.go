package main

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"unsafe"
)

//go:generate goversioninfo -gofile=versioninfo.go

const (
	ERROR_ALREADY_EXISTS = 183
	JDK_PATH             = "\\jre\\bin\\javaw.exe"
	JAR_FILE             = "\\zxpoly-emul.jar"
	MUTEX                = ""
)

var (
	kernel32        = syscall.NewLazyDLL("kernel32.dll")
	procCreateMutex = kernel32.NewProc("CreateMutexW")
)

func createMutex(name string) (uintptr, error) {
	ret, _, err := procCreateMutex.Call(
		0,
		0,
		uintptr(unsafe.Pointer(syscall.StringToUTF16Ptr(name))),
	)
	switch int(err.(syscall.Errno)) {
	case 0:
		return ret, nil
	default:
		return ret, err
	}
}

func main() {
	fmt.Printf("%v %v\n%v\n", versionInfo.StringFileInfo.ProductName, versionInfo.StringFileInfo.ProductVersion, versionInfo.StringFileInfo.LegalCopyright)
	if MUTEX != "" {
		fmt.Printf("Trying create mutex\n")
		_, err := createMutex(MUTEX)
		if err != nil {
			if n := int(err.(syscall.Errno)); n != ERROR_ALREADY_EXISTS {
				fmt.Printf("CreateMutex error should be %d, got %d", ERROR_ALREADY_EXISTS, n)
				log.Fatal(err)
			} else {
				fmt.Printf("Application already started....")
				os.Exit(1)
			}
		}
	}

	path, err := os.Executable()
	if err == nil {
		base_folder := filepath.Dir(path)

		cmd := exec.Command(base_folder+JDK_PATH,
			"-XX:+UseZGC",
			"-XX:+TieredCompilation",
			"-XX:MaxMetaspaceSize=128m",
			"-Xms512M",
			"-Xmx1G",
			"-Dsun.rmi.transport.tcp.maxConnectionThreads=0",
			"-XX:-DontCompileHugeMethods",
			"-XX:+DisableAttachMechanism",
			"-Xverify:none",
			"-Dsun.java2d.d3d=true",
			"-Dsun.java2d.ddoffscreen=true",
			"-Dsun.java2d.ddforcevram=true",
			"-Dsun.java2d.ddscale=true",
			"-Dsun.java2d.accthreshold=0",
			"-Djava.library.path="+base_folder,
			"-jar", base_folder+JAR_FILE)
		fmt.Printf("Application starting...\n")

		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr

		err = cmd.Start()
		if err != nil {
			log.Fatal(err)
		}

		fmt.Printf("Waiting application completion...\n")

		err = cmd.Wait()
		if err != nil {
			fmt.Printf("Application completed with status: %v", err)
			log.Fatal(err)
		}
	} else {
		log.Fatal(err)
	}
}
