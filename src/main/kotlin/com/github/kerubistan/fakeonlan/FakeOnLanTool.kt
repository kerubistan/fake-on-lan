package com.github.kerubistan.fakeonlan

import org.libvirt.Connect
import org.libvirt.DomainInfo
import java.lang.Byte
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class FakeOnLanTool(val connect: Connect) {

	fun run() {
		val buffer = ByteArray(1024)
		val socket = DatagramSocket(9, InetAddress.getByName("0.0.0.0"))
		while (true) {
			socket.receive(DatagramPacket(buffer, buffer.size))
			val isMagicPacket =
					(0..5).all { buffer[it] == (0xFF.toByte()) }
							&& (1..15).all { buffer[it * 6] == buffer[6 + (it * 6)] }
			if (isMagicPacket) {
				val mac = buffer.copyOfRange(6, 12)
				val formattedMac = macToString(mac).toLowerCase()
				println("magic packet for $formattedMac")
				for (domName in connect.listDefinedDomains()) {
					val domain = connect.domainLookupByName(domName)
					if (domain.getXMLDesc(0).toLowerCase().contains(formattedMac)
							&& domain.info.state == DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
						println(" starting $domName")
						domain.create()
					}
				}
			} else {
				println("no magic: ignored")
			}

		}
	}

	companion object {

		val hexaDigits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

		fun macToString(mac: ByteArray): String {
			require(mac.size == 6, { "The MAC address must be 6 bytes" })
			return buildString(17) {
				for (b in mac) {
					if (length != 0) {
						append(':')
					}
					Byte.toUnsignedInt(b)
					append(hexaDigits[Byte.toUnsignedInt(b) and 0xF0 ushr 4])
					append(hexaDigits[Byte.toUnsignedInt(b) and 0x0F])
				}
			}
		}


		@JvmStatic fun main(args: Array<String>) {
			val hypervisor = args.getOrElse(0) { "qemu:///system" }
			println("connecting $hypervisor")
			FakeOnLanTool(Connect(hypervisor)).run()
			//return 0
		}
	}
}