package com.aqnichol.ftproxy;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

public class PacketValidator {

	public class InvalidPacketException extends Exception {
		private static final long serialVersionUID = -2286046215636730634L;
		private Map<?, ?> packet;

		public InvalidPacketException (String message, Map<?, ?> packet) {
			super(message);
			this.packet = packet;
		}

		public Map<?, ?> getPacket () {
			return packet;
		}
	}

	private Map<?, ?> map;
	
	public PacketValidator (Map<?, ?> packet) {
		map = packet;
	}

	public void validateGeneralPacket () throws PacketValidator.InvalidPacketException {
		Set<?> keys = map.keySet();
		for (Object key : keys) {
			if (!(key instanceof String)) {
				throw new InvalidPacketException("Invalid key: " + key, map);
			}
		}
		if (!map.containsKey("type")) {
			throw new InvalidPacketException("Missing type field", map);
		}
		Object typeObject = map.get("type");
		if (!(typeObject instanceof String)) {
			throw new InvalidPacketException("Invalid class for type field: " + typeObject.getClass(), map);
		}
		String typeName = (String)typeObject;
		if (typeName.equals("auth")) {
			validateClientAuthMap();
		} else if (typeName.equals("data")) {
			validateClientDataMap();
		} else {
			throw new InvalidPacketException("Unknown type field: \"" + typeName + "\"", map);
		}
	}
	
	public void validateClientAuthMap () throws PacketValidator.InvalidPacketException {
		if (!map.containsKey("token")) {
			throw new InvalidPacketException("No token field found", map);
		}
		Object tokenObj = map.get("token");
		if (!(tokenObj instanceof ByteBuffer)) {
			throw new InvalidPacketException("Invalid class for token field: " + tokenObj.getClass(), map);
		}
	}
	
	public void validateClientDataMap () throws PacketValidator.InvalidPacketException {
		if (!map.containsKey("data")) {
			throw new InvalidPacketException("No data field found", map);
		}
		// Allow sending ANY kind of object
		//Object dataObj = map.get("data");
		//if (!(dataObj instanceof ByteBuffer)) {
		//	throw new InvalidPacketException("Invalid class for data field: " + dataObj.getClass(), map);
		//}
	}

}
