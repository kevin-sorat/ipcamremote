package com.ksorat.foscamremote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Formatter;

import com.ksorat.ipcamremote.tcpCommand.AudioData;
import com.ksorat.ipcamremote.tcpCommand.IPCameraTCPCommand;
import com.ksorat.ipcamremote.tcpCommand.LoginRequest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public abstract class AudioAsyncTask extends AsyncTask<String, Void, String> {

	protected static final String TAG = "IPCamRemote:AudioAsyncTask";
	protected static final String MESSAGE = "Received an exception";

	protected Socket socket;
	protected OutputStream out;
	protected DataOutputStream dos;
	protected InputStream in;
	protected DataInputStream dis;

	protected static int connectionID;

	protected Socket socket2;
	protected OutputStream out2;
	protected DataOutputStream dos2;
	protected InputStream in2;
	protected DataInputStream dis2;

	protected FileInputStream fis;

	public Context context;
	public boolean isDone = false;

	private int predictedSample = 0;
	private int stepIndex = 0;
	private int stepSize = 7;
	
	static final int[] indexTable = { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1,
			-1, 2, 4, 6, 8, };

	private int[] stepTable = new int[] { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
			19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88,
			97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
			337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060,
			1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
			3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630,
			9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350,
			22385, 24623, 27086, 29794, 32767 };

	protected void closeSockets() {
		try {
			// Close connection
			if (socket != null) {
				socket.close();
			}

			if (socket2 != null) {
				socket2.close();
			}

			if (fis != null) {
				fis.close();
			}
		} catch (IOException e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	protected void sendTCPRequest(byte[] myByteArray, DataOutputStream dos) throws Exception {
		int len = myByteArray.length;
		if (len > 0 && !socket.isClosed()) {
			dos.write(myByteArray, 0, len);
			dos.flush();
			/*
			 * try { Thread.sleep(1000); } catch (InterruptedException e) {
			 * e.printStackTrace(); }
			 */
		}
	}

	protected boolean validateTCPLoginResponse(DataInputStream dis) {
		try {
			byte[] headerData = new byte[LoginRequest.HEADER_LENGTH];
			// Skip the header section
			dis.read(headerData);
			// Get the result code (0 = ok)
			short result = dis.readShort();
			byte[] cameraID = new byte[13];
			dis.read(cameraID);
			dis.skipBytes(8);
			byte[] firmwareVersion = new byte[4];
			dis.read(firmwareVersion);

			if (result == 0) {
				return true;
			} else {
				// There is a problem
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return false;
		}
		return false;
	}

	protected int validateTCPGetConfiguredCameraResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// If the opcode is not the expected one
			if (opcode != 28) {
				// Skip 9 bytes
				byte[] retain = new byte[9];
				dis.readFully(retain);
				// hexStr = bytesToHexString(retain);

				// Get body length
				byte[] bodyLengthBytes = new byte[4];
				dis.readFully(bodyLengthBytes);
				// hexStr = bytesToHexString(bodyLengthBytes);

				int bodyLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(bodyLengthBytes);
				// Repeat again once more
				dis.readFully(bodyLengthBytes);
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);

				return validateTCPGetConfiguredCameraResponse(dis);

				// Else the opcode is the expected one
			} else {
				// Skip to the body
				byte[] skip17Bytes = new byte[17];
				dis.readFully(skip17Bytes);

				return -1;
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
	}

	protected boolean validateTCPVerifyResponse(DataInputStream dis) {
		try {
			byte[] headerData = new byte[LoginRequest.HEADER_LENGTH];
			// Skip the header section
			dis.readFully(headerData);
			// Get the result code (0 = ok)
			short result = dis.readShort();
			dis.readByte();

			if (result == 0) {
				return true;
			} else if (result == 1) {
				// Username is invalid
			} else if (result == 5) {
				// Password is invalid
			} else {
				// Other errors
			}
		} catch (IOException e) {
			Log.e(TAG, MESSAGE, e);
			return false;
		}
		return false;
	}

	protected int validateVideoStartResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// If the opcode is not the expected one
			if (opcode != 5) {
				// Skip 9 bytes
				byte[] retain = new byte[9];
				dis.readFully(retain);
				// hexStr = bytesToHexString(retain);

				// Get body length
				byte[] bodyLengthBytes = new byte[4];
				dis.readFully(bodyLengthBytes);
				// hexStr = bytesToHexString(bodyLengthBytes);

				int bodyLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(bodyLengthBytes);
				// Repeat again once more
				dis.readFully(bodyLengthBytes);
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);

				return validateVideoStartResponse(dis);

				// Else the opcode is the expected one
			} else {
				// Skip to the body
				byte[] skip17Bytes = new byte[17];
				dis.readFully(skip17Bytes);
				// Get the result code (0 = ok)
				byte[] resultBytes = new byte[2];
				dis.readFully(resultBytes);
				short result = IPCameraTCPCommand
						.bytesToShortLittleEndian(resultBytes);
				// hexStr = bytesToHexString(resultBytes);

				if (result == 0) {
					// Get connection ID
					byte[] connIDBytes = new byte[4];
					dis.readFully(connIDBytes);
					int connID = IPCameraTCPCommand
							.bytesToIntLittleEndian(connIDBytes);
					// hexStr = bytesToHexString(connIDBytes);

					// int connID = dis.readInt();
					return connID;
				} else if (result == 2) {
					// Refused due to over the the maximum number of connections
				} else {
					// Other errors
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
		return -1;
	}

	protected int validateAudioStartResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// If the opcode is not the expected one
			if (opcode != 9) {
				// Skip 9 bytes
				byte[] retain = new byte[9];
				dis.readFully(retain);
				// hexStr = bytesToHexString(retain);

				// Get body length
				byte[] bodyLengthBytes = new byte[4];
				dis.readFully(bodyLengthBytes);
				// hexStr = bytesToHexString(bodyLengthBytes);

				int bodyLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(bodyLengthBytes);
				// Repeat again once more
				dis.readFully(bodyLengthBytes);
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);

				return validateAudioStartResponse(dis);

				// Else the opcode is the expected one
			} else {
				// Skip to the body
				byte[] skip17Bytes = new byte[17];
				dis.readFully(skip17Bytes);
				// Get the result code (0 = ok)
				byte[] resultBytes = new byte[2];
				dis.readFully(resultBytes);
				short result = IPCameraTCPCommand
						.bytesToShortLittleEndian(resultBytes);
				// hexStr = bytesToHexString(resultBytes);

				if (result == 0) {
					// Get connection ID
					byte[] connIDBytes = new byte[4];
					dis.readFully(connIDBytes);
					int connID = IPCameraTCPCommand
							.bytesToIntLittleEndian(connIDBytes);
					// hexStr = bytesToHexString(connIDBytes);

					// int connID = dis.readInt();
					return connID;
				} else if (result == 2) {
					// Refused due to over the the maximum number of connections
				} else if (result == 7) {
					// Camera do not support this function
				} else {
					// Other errors
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
		return -1;
	}

	protected int validateTalkStartResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// If the opcode is not the expected one
			if (opcode != 12) {
				// Skip 9 bytes
				byte[] retain = new byte[9];
				dis.readFully(retain);
				// hexStr = bytesToHexString(retain);

				// Get body length
				byte[] bodyLengthBytes = new byte[4];
				dis.readFully(bodyLengthBytes);
				// hexStr = bytesToHexString(bodyLengthBytes);

				int bodyLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(bodyLengthBytes);
				// Repeat again once more
				dis.readFully(bodyLengthBytes);
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);

				return validateTalkStartResponse(dis);

				// Else the opcode is the expected one
			} else {
				// Skip to the body
				byte[] skip17Bytes = new byte[17];
				dis.readFully(skip17Bytes);
				// Get the result code (0 = ok)
				byte[] resultBytes = new byte[2];
				dis.readFully(resultBytes);
				short result = IPCameraTCPCommand
						.bytesToShortLittleEndian(resultBytes);
				// hexStr = bytesToHexString(resultBytes);

				if (result == 0) {
					// Get connection ID
					byte[] connIDBytes = new byte[4];
					dis.readFully(connIDBytes);
					int connID = IPCameraTCPCommand
							.bytesToIntLittleEndian(connIDBytes);
					// hexStr = bytesToHexString(connIDBytes);

					// int connID = dis.readInt();
					return connID;
				} else if (result == 2) {
					// Refused due to over the the maximum number of connections
				} else if (result == 7) {
					// Camera do not support this function
				} else {
					// Other errors
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
		return -1;
	}

	protected byte[] validateVideoDataResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// Skip 9 bytes
			byte[] retain = new byte[9];
			dis.readFully(retain);
			// hexStr = bytesToHexString(retain);

			// Get body length
			byte[] bodyLengthBytes = new byte[4];
			dis.readFully(bodyLengthBytes);
			// hexStr = bytesToHexString(bodyLengthBytes);

			int bodyLength = IPCameraTCPCommand
					.bytesToIntLittleEndian(bodyLengthBytes);
			// Repeat again once more
			dis.readFully(bodyLengthBytes);

			// If the opcode is not the expected one
			if (opcode != 1) {
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);
				return validateVideoDataResponse(dis);
				// Else the opcode is the expected one
			} else {
				byte[] videoDataBytes = new byte[bodyLength];
				dis.readFully(videoDataBytes);
				// hexStr = bytesToHexString(videoDataBytes);
				return videoDataBytes;
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return null;
		}
	}

	protected AudioData validateAudioDataResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);
			// String hexStr = bytesToHexString(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);
			// hexStr = bytesToHexString(opcodeBytes);

			// Skip 9 bytes
			byte[] retain = new byte[9];
			dis.readFully(retain);
			// hexStr = bytesToHexString(retain);

			// Get body length
			byte[] bodyLengthBytes = new byte[4];
			dis.readFully(bodyLengthBytes);
			// hexStr = bytesToHexString(bodyLengthBytes);

			int bodyLength = IPCameraTCPCommand
					.bytesToIntLittleEndian(bodyLengthBytes);
			// Repeat again once more
			dis.readFully(bodyLengthBytes);

			// If the opcode is not the expected one
			if (opcode != 2) {
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);
				// hexStr = bytesToHexString(bytesToSkip);
				return validateAudioDataResponse(dis);
				// Else the opcode is the expected one
			} else {
				// Retrieve audio data in bytes

				// Create AudioData object to store
				AudioData audioData = new AudioData();

				byte[] timeStampBytes = new byte[4];
				dis.readFully(timeStampBytes);
				// hexStr = bytesToHexString(timeStampBytes);
				audioData.set_timestamp(IPCameraTCPCommand
						.bytesToIntLittleEndian(timeStampBytes));

				byte[] packetSeqNumBytes = new byte[4];
				dis.readFully(packetSeqNumBytes);
				// hexStr = bytesToHexString(packetSeqNumBytes);
				audioData.set_packSeqNum(IPCameraTCPCommand
						.bytesToIntLittleEndian(packetSeqNumBytes));

				byte[] aquisitionTimeBytes = new byte[4];
				dis.readFully(aquisitionTimeBytes);
				// hexStr = bytesToHexString(aquisitionTimeBytes);
				audioData.set_aquisitionTime(IPCameraTCPCommand
						.bytesToIntLittleEndian(aquisitionTimeBytes));

				audioData.set_format(dis.readByte());

				byte[] dataLengthBytes = new byte[4];
				dis.readFully(dataLengthBytes);
				// hexStr = bytesToHexString(dataLengthBytes);
				int dataLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(dataLengthBytes);
				audioData.set_dataLength(dataLength);

				byte[] audioDataContentBytes = new byte[dataLength];
				dis.readFully(audioDataContentBytes);
				// hexStr = bytesToHexString(audioDataContentBytes);
				// audioData.setDataContent(IPCameraTCPCommand.bytesToBytesLittleEndian(audioDataContentBytes));
				audioData.setDataContent(audioDataContentBytes);

				return audioData;
			}
		} catch (Exception e) {
			closeSockets();
			Log.e(TAG, MESSAGE, e);
			return null;
		}
	}
	
	

	protected byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	protected static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);

		Formatter formatter = new Formatter(sb);

		for (byte b : bytes) {
			formatter.format("%02x ", b);
		}
		formatter.close();

		return sb.toString();
	}

	public byte[] decompress(byte[] data) {
		ArrayList<Short> list = new ArrayList<Short>();

		data = Bit8ToBit4(data);

		int difference = 0;
		// int newSample = 0;

		for (int i = 0; i < data.length; i++) {
			difference = 0;

			if ((data[i] & 4) != 0)
				difference += stepSize;
			if ((data[i] & 2) != 0)
				difference += stepSize >> 1;
			if ((data[i] & 1) != 0)
				difference += stepSize >> 2;
			difference += stepSize >> 3;

			if ((data[i] & 8) != 0)
				difference = -difference;
			predictedSample += difference;

			if (predictedSample > 32767)
				predictedSample = 32767;
			else if (predictedSample < -32768)
				predictedSample = -32768;

			list.add((short) predictedSample);

			stepIndex += indexTable[data[i]];
			if (stepIndex < 0)
				stepIndex = 0;
			else if (stepIndex > 88)
				stepIndex = 88;
			stepSize = stepTable[stepIndex];
		}

		byte[] result = new byte[list.size() * 2];
		int tempIndex = 0;
		for (int j = 0; j < list.size(); j++) {
			byte[] twoBytes = shortToByte(list.get(j));
			result[tempIndex++] = twoBytes[0];
			result[tempIndex++] = twoBytes[1];
		}
		return result;
	}

	public int compress(short[] input, int inp, int len, byte[] output, int outp) {
		int sign;
		int delta;
		int vpdiff;
		int step = stepTable[stepIndex];
		int outputbuffer = 0;
		int bufferstep = 1;
		
		// Get rid of noises/static
		if (predictedSample < 256 && predictedSample > -256)
			predictedSample = 0;
		
		output[outp] = (byte) ((short)predictedSample & 0x00ff);
		output[outp + 1] = (byte) (((short)predictedSample >> 8) & 0x00ff);
		output[outp + 2] = (byte) stepIndex;
		output[outp + 3] = (byte) 0;
		outp += 4;

		int count = len;
		while (--count >= 0) {

			delta = input[inp++] - predictedSample;
			sign = (delta < 0) ? 8 : 0;
			if (0 != sign)
				delta = (-delta);

			int tmp = 0;
			vpdiff = step >> 3;
			if (delta > step) {
				tmp = 4;
				delta -= step;
				vpdiff += step;
			}
			step >>= 1;
			if (delta > step) {
				tmp |= 2;
				delta -= step;
				vpdiff += step;
			}
			step >>= 1;
			if (delta > step) {
				tmp |= 1;
				vpdiff += step;
			}
			delta = tmp;

			if (0 != sign)
				predictedSample -= vpdiff;
			else
				predictedSample += vpdiff;

			if (predictedSample > 32767)
				predictedSample = 32767;
			else if (predictedSample < -32768)
				predictedSample = -32768;

			delta |= sign;

			stepIndex += indexTable[delta];
			if (stepIndex < 0)
				stepIndex = 0;
			if (stepIndex > 88)
				stepIndex = 88;
			step = stepTable[stepIndex];

			if (0 != bufferstep) {
				outputbuffer = (delta << 4) & 0xf0;
			} else {
				output[outp++] = (byte) ((delta & 0x0f) | outputbuffer);
			}
			bufferstep = (0 == bufferstep) ? 1 : 0;
		}

		if (0 == bufferstep)
			output[outp++] = (byte) outputbuffer;

		return (len / 2) + 4;
	}

	private byte[] Bit8ToBit4(byte[] data) {
		byte[] bit4 = new byte[data.length * 2];

		int j = 0;
		for (int i = 0; i < data.length; i++) {
			bit4[j++] = (byte) (data[i] & 0x0F);
			bit4[j++] = (byte) ((data[i] & 0xF0) >> 4);
		}
		return bit4;
	}

	/*
	private byte[] Bit4ToBit8(byte[] data) {
		byte[] bit8 = new byte[data.length / 2];

		int j = 0;
		for (int i = 0; i < data.length; i += 2) {
			byte byte1 = data[i];
			byte byte2 = 0;
			if (i + 1 < data.length)
				byte2 = (byte) (data[i + 1] << 4);
			bit8[j++] = ((byte) (byte1 + byte2));
		}
		return bit8;
	}
	*/

	private byte[] shortToByte(short value) {
		byte[] result = new byte[2];
		// short to bytes conversion
		result[0] = (byte) (value & 0x00ff);
		result[1] = (byte) ((value >> 8) & 0x00ff);

		return result;
	}

	/*
	private int byteToShort(byte byte1, byte byte2) {
		return ((byte2 & 0xFF) << 8) | (byte1 & 0xFF);
	}
	*/
}
