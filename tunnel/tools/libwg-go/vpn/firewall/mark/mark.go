package mark

const (
	// LinuxFwmarkMaskNum Used to isolate bits 16-23 which are the safe range for custom marks
	LinuxFwmarkMaskNum = 0xff0000
	// Our mark num
	LinuxBypassMarkNum = 0x100000
	// LinuxBootstrapMarkNum is specifically for the DNS Resolver
	LinuxBootstrapMarkNum = 0x200000
)

var (
	// LinuxBootstrapMarkBytes is the Little Endian representation for nftables
	// 0x200000 -> [00, 00, 20, 00]
	LinuxBootstrapMarkBytes = []byte{0x00, 0x00, 0x20, 0x00}
)
