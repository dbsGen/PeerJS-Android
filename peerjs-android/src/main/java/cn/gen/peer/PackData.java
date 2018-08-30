package cn.gen.peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mac on 2018/3/5.
 */

public class PackData {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_NIL = 1;
    public static final int TYPE_BYTE = 2;
    public static final int TYPE_SHORT = 3;
    public static final int TYPE_INTEGER = 4;
    public static final int TYPE_LONG = 5;
    public static final int TYPE_BUFFER = 6;
    public static final int TYPE_STRING = 7;
    public static final int TYPE_ARRAY = 8;
    public static final int TYPE_MAP = 9;
    public static final int TYPE_BOOL = 10;
    public static final int TYPE_FLOAT = 11;
    public static final int TYPE_DOUBLE = 12;

    public static class PackException extends Exception {
        public PackException(String msg) {
            super(msg);
        }
    }

    private Object data;
    private int type = TYPE_NONE;

    private static int readUint8(ByteBuffer buffer) {
        return buffer.get()&0xff;
    }
    private static int readUint16(ByteBuffer buffer) {
        byte[] bytes = new byte[2];
        buffer.get(bytes);
        int ret = 0;
        for (byte b : bytes) {
            ret = ret * 256 + (b & 0xff);
        }
        return ret;
    }
    private static int readUint32(ByteBuffer buffer) {
        byte[] bytes = new byte[4];
        buffer.get(bytes);
        int ret = 0;
        for (byte b : bytes) {
            ret = ret * 256 + (b & 0xff);
        }
        return ret;
    }

    private static int readUint64(ByteBuffer buffer) {
        byte[] bytes = new byte[8];
        buffer.get(bytes);
        int ret = 0;
        for (byte b : bytes) {
            ret = ret * 256 + (b & 0xff);
        }
        return ret;
    }

    public PackData(ByteBuffer buffer) throws IOException {
        int type = readUint8(buffer);
        if (type < 0x80) {
            data = (byte)type;
            this.type = TYPE_BYTE;
            return;
        }else if ((type ^ 0xe0) < 0x20) {
            char negative_fixnum = (char)((type ^ 0xe0) - 0x20);
            data = (byte)negative_fixnum;
            this.type = TYPE_BYTE;
            return;
        }
        int size;
        if ((size = type ^ 0xa0) <= 0x0f) {
            byte[] b = new byte[size];
            buffer.get(b);
            data = b;
            this.type = TYPE_BUFFER;
            return;
        }else if ((size = type ^ 0xb0) <= 0x0f) {
            byte[] b = new byte[size];
            buffer.get(b);
            data = new String(b);
            this.type = TYPE_STRING;
            return;
        }else if ((size = type ^ 0x90) <= 0x0f) {
            ArrayList<PackData> datas = new ArrayList<>();
            for (int i = 0; i < size; ++i) {
                datas.add(new PackData(buffer));
            }
            data = datas;
            this.type = TYPE_ARRAY;
            return;
        }else if ((size = type ^ 0x80) <= 0x0f) {
            HashMap map = new HashMap();
            for (int i = 0; i < size; ++i) {
                PackData key = new PackData(buffer);
                PackData value = new PackData(buffer);
                map.put(key.get(), value);
            }
            data = map;
            this.type = TYPE_MAP;
            return;
        }
        switch(type){
            case 0xc0:
                data = null;
                this.type = TYPE_NIL;
                break;
            case 0xc1:
                data = null;
                this.type = TYPE_NONE;
                break;
            case 0xc2:
                data = false;
                this.type = TYPE_BOOL;
                break;
            case 0xc3:
                data = true;
                this.type = TYPE_BOOL;
                break;
            case 0xca:
                data = buffer.getFloat();
                this.type = TYPE_FLOAT;
                break;
            case 0xcb:
                data = buffer.getDouble();
                this.type = TYPE_DOUBLE;
                break;
            case 0xcc:
                data = buffer.get();
                this.type = TYPE_BYTE;
                break;
            case 0xcd:
                data = buffer.getShort();
                this.type = TYPE_SHORT;
                break;
            case 0xce:
                data = buffer.getInt();
                this.type = TYPE_INTEGER;
                break;
            case 0xcf:
                data = buffer.getLong();
                this.type = TYPE_LONG;
                break;
            case 0xd0:
                data = buffer.get();
                this.type = TYPE_BYTE;
                break;
            case 0xd1:
                data = buffer.getShort();
                this.type = TYPE_SHORT;
                break;
            case 0xd2:
                data = buffer.getInt();
                this.type = TYPE_INTEGER;
                break;
            case 0xd3:
                data = buffer.getLong();
                this.type = TYPE_LONG;
                break;
            case 0xd4:
                data = null;
                this.type = TYPE_NONE;
                break;
            case 0xd5:
                data = null;
                this.type = TYPE_NONE;
                break;
            case 0xd6:
                data = null;
                this.type = TYPE_NONE;
                break;
            case 0xd7:
                data = null;
                this.type = TYPE_NONE;
                break;
            case 0xd8: {
                size = readUint16(buffer);
                byte[] b = new byte[size];
                buffer.get(b);
                data = new String(b);
                this.type = TYPE_STRING;
                break;
            }
            case 0xd9: {
                size = readUint32(buffer);
                byte[] b = new byte[size];
                buffer.get(b);
                data = new String(b);
                this.type = TYPE_STRING;
                break;
            }
            case 0xda: {
                size = readUint16(buffer);
                byte[] b = new byte[size];
                buffer.get(b);
                data = b;
                this.type = TYPE_BUFFER;
                break;
            }
            case 0xdb: {
                size = readUint32(buffer);
                byte[] b = new byte[size];
                buffer.get(b);
                data = b;
                this.type = TYPE_BUFFER;
                break;
            }
            case 0xdc: {
                size = readUint16(buffer);
                ArrayList<PackData> datas = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    datas.add(new PackData(buffer));
                }
                data = datas;
                this.type = TYPE_ARRAY;
                break;
            }
            case 0xdd: {
                size = readUint32(buffer);
                ArrayList<PackData> datas = new ArrayList<>();
                for (int i = 0; i < size; ++i) {
                    datas.add(new PackData(buffer));
                }
                data = datas;
                this.type = TYPE_ARRAY;
                break;
            }
            case 0xde: {
                size = readUint16(buffer);
                HashMap map = new HashMap();
                for (int i = 0; i < size; ++i) {
                    PackData key = new PackData(buffer);
                    PackData value = new PackData(buffer);
                    map.put(key.get(), value);
                }
                data = map;
                this.type = TYPE_MAP;
                break;
            }
            case 0xdf: {
                size = readUint32(buffer);
                HashMap map = new HashMap();
                for (int i = 0; i < size; ++i) {
                    PackData key = new PackData(buffer);
                    PackData value = new PackData(buffer);
                    map.put(key.get(), value);
                }
                data = map;
                this.type = TYPE_MAP;
                break;
            }
        }
    }
    private PackData(List<PackData> list) {
        data = list;
        type = TYPE_ARRAY;
    }

    public PackData() {

    }

    public static PackData unpack(ByteBuffer buffer) {
        try {
            return new PackData(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getType() {
        return type;
    }

    public Object get() {
        return data;
    }

    public PackData get(String key) {
        if (type == TYPE_MAP) {
            HashMap map = (HashMap)data;
            if (map.containsKey(key)) {
                return (PackData)map.get(key);
            }
        }
        return null;
    }
    public String getString(String key) {
        PackData data = get(key);
        if (data != null) {
            return data.stringValue();
        }
        return null;
    }
    public int getInt(String key) {
        PackData data = get(key);
        if (data != null) {
            return data.intValue();
        }
        return 0;
    }

    public PackData get(int idx) {
        if (type == TYPE_ARRAY) {
            ArrayList<PackData> arr = (ArrayList<PackData>)data;
            return arr.get(idx);
        }
        return null;
    }

    private void _toString(StringBuilder stringBuilder) {
        if (data == null) {
            stringBuilder.append("null");
        }else {
            switch (type) {
                case TYPE_MAP: {
                    HashMap map = (HashMap)data;
                    Set keys = map.keySet();
                    stringBuilder.append('{');
                    int size = keys.size();
                    if (size > 0) {
                        int count = 0;
                        for (Object key: keys) {
                            stringBuilder.append(key.toString());
                            stringBuilder.append(":");
                            PackData d = (PackData) map.get(key);
                            d._toString(stringBuilder);
                            if (++count < size) {
                                stringBuilder.append(',');
                            }
                        }
                    }
                    stringBuilder.append('}');
                    break;
                }
                case TYPE_ARRAY: {
                    ArrayList<PackData> arr = (ArrayList<PackData>)data;
                    int size = arr.size();
                    stringBuilder.append("[");
                    for (int i = 0; i < size; ++i) {
                        arr.get(i)._toString(stringBuilder);
                        if (i + 1 < size) {
                            stringBuilder.append(",");
                        }
                    }
                    stringBuilder.append("]");
                    break;
                }
                case TYPE_STRING: {
                    stringBuilder.append("\"");
                    stringBuilder.append((String) data);
                    stringBuilder.append("\"");
                    break;
                }
                default: {
                    stringBuilder.append(data.toString());
                }
            }
        }

    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        _toString(builder);
        return builder.toString();
    }

    public void set(Object data, int type) {
        this.data = data;
        this.type = type;
    }

    public int intValue() {
        switch (type) {
            case TYPE_INTEGER: {
                return (int)data;
            }
            case TYPE_BOOL: {
                return (boolean)data ? 1 : 0;
            }
            case TYPE_BYTE: {
                return (byte)data & 0xff;
            }
            case TYPE_FLOAT: {
                return (int)(float)data;
            }
            case TYPE_DOUBLE: {
                return (int)(double)data;
            }
            case TYPE_SHORT: {
                return (short)data & 0xffff;
            }
            case TYPE_LONG: {
                return (int)((long)data);
            }
            default: {
                return 0;
            }
        }
    }
    public String stringValue() {
        if (type == TYPE_STRING) {
            return (String)data;
        }
        return null;
    }
    public byte[] bufferValue() {
        if (type == TYPE_BUFFER) {
            return (byte[]) data;
        }
        return null;
    }

    public void set(String string) {set(string, TYPE_STRING);}
    public void set(byte b) {set(b, TYPE_BYTE);}
    public void set(short s) {set(s, TYPE_SHORT);}
    public void set(int i) {set(i, TYPE_INTEGER);}
    public void set(long l) {set(l, TYPE_LONG);}
    public void set(byte[] b) {set(b, TYPE_BUFFER);}
    public void set(Collection a) {set(new ArrayList(a), TYPE_ARRAY);}
    public void set(Map m) {set(new HashMap(m), TYPE_MAP);}
    public void set(boolean b) {set(b, TYPE_BOOL);}
    public void set(float f) {set(f, TYPE_FLOAT);}
    public void set(double d) {set(d, TYPE_DOUBLE);}

    public PackData(String v) {set(v);}
    public PackData(byte v) {set(v);}
    public PackData(short v) {set(v);}
    public PackData(int v) {set(v);}
    public PackData(long v) {set(v);}
    public PackData(byte[] v) {set(v);}
    public PackData(Collection v) {set(v);}
    public PackData(Map v) {set(v);}
    public PackData(boolean v) {set(v);}
    public PackData(float v) {set(v);}
    public PackData(double v) {set(v);}

    public void put(String key, PackData value) throws PackException {
        if (type == TYPE_NONE) {
            type = TYPE_MAP;
            data = new HashMap<>();
        }
        if (type == TYPE_MAP) {
            ((HashMap)data).put(key, value);
        }else {
            throw new PackException("This PackData is not a map.");
        }
    }
    public void put(String key, String v) throws PackException {put(key, new PackData(v));}
    public void put(String key, byte v) throws PackException {put(key, new PackData(v));}
    public void put(String key, short v) throws PackException {put(key, new PackData(v));}
    public void put(String key, int v) throws PackException {put(key, new PackData(v));}
    public void put(String key, long v) throws PackException {put(key, new PackData(v));}
    public void put(String key, byte[] v) throws PackException {put(key, new PackData(v));}
    public void put(String key, Collection v) throws PackException {put(key, new PackData(v));}
    public void put(String key, Map v) throws PackException {put(key, new PackData(v));}
    public void put(String key, boolean v) throws PackException {put(key, new PackData(v));}
    public void put(String key, float v) throws PackException {put(key, new PackData(v));}
    public void put(String key, double v) throws PackException {put(key, new PackData(v));}

    public void add(PackData value) throws PackException {
        if (type == TYPE_NONE) {
            type = TYPE_ARRAY;
            data = new ArrayList<>();
        }
        if (type == TYPE_ARRAY) {
            ((ArrayList)data).add(value);
        }else {
            throw new PackException("This PackData is not a array.");
        }
    }
    public void add(String v) throws PackException {add(new PackData(v));}
    public void add(byte v) throws PackException {add(new PackData(v));}
    public void add(short v) throws PackException {add(new PackData(v));}
    public void add(int v) throws PackException {add(new PackData(v));}
    public void add(long v) throws PackException {add(new PackData(v));}
    public void add(byte[] v) throws PackException {add(new PackData(v));}
    public void add(Collection v) throws PackException {add(new PackData(v));}
    public void add(Map v) throws PackException {add(new PackData(v));}
    public void add(boolean v) throws PackException {add(new PackData(v));}
    public void add(float v) throws PackException {add(new PackData(v));}
    public void add(double v) throws PackException {add(new PackData(v));}

    public int size() {
        switch (type) {
            case TYPE_MAP:
                return ((HashMap)data).size();
            case TYPE_ARRAY:
                return ((ArrayList)data).size();
            default:
                return 1;
        }
    }

    private static void packUint16(OutputStream out, int value) throws IOException {
        out.write(new byte[]{(byte)(value>>8), (byte)(value&0xff)});
    }
    private static void packInt16(OutputStream out, short value) throws IOException {
        out.write(new byte[]{(byte)(value>>8), (byte)(value&0xff)});
    }
    private static void packUint32(OutputStream out, int value) throws IOException {
        out.write(new byte[]{
                (byte)((value & 0xff000000) >> 24),
                (byte)((value & 0x00ff0000) >> 16),
                (byte)((value & 0x0000ff00) >> 8),
                (byte)(value & 0x000000ff)
        });
    }
    private static void packInt64(OutputStream out, long value) throws IOException {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[i] = (byte) (value >>> (56 - i * 8));
        }
        out.write(b);
    }

    private static void packString(OutputStream out, String value) throws PackException, IOException {
        int length = value.length();
        if (length <= 0x0f) {
            out.write(new byte[]{(byte)(0xb0+length)});
        }else if (length <= 0xffff) {
            out.write(new byte[]{(byte)0xd8});
            packUint16(out, length);
        }else {
            out.write(new byte[]{(byte)0xd9});
            packUint32(out, length);
        }
        out.write(value.getBytes());
    }

    public ByteBuffer pack() throws PackException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pack(out);
        byte[] bytes = out.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.rewind();
        out.close();
        return buffer;
    }

    public void pack(OutputStream out) throws PackException, IOException {

        switch (type) {
            case TYPE_STRING: {
                packString(out, (String) data);
                break;
            }
            case TYPE_BYTE: {
                byte num = (byte)data;
                out.write(new byte[]{(byte)0xd0, num});
                break;
            }
            case TYPE_SHORT: {
                short num = (short)data;
                out.write(new byte[]{(byte)0xd1});
                packInt16(out, num);
                break;
            }
            case TYPE_INTEGER: {
                int num = (int)data;
                out.write(new byte[]{(byte)0xd2});
                packUint32(out, num);
                break;
            }
            case TYPE_LONG: {
                long num = (long)data;
                out.write(new byte[]{(byte)0xd3});
                packInt64(out, num);
                break;
            }
            case TYPE_FLOAT: {
                float num = (float)data;
                out.write(new byte[]{(byte)0xca});
                out.write(ByteBuffer.allocate(4).putFloat(num).array());
                break;
            }
            case TYPE_DOUBLE: {
                double num = (double)data;
                out.write(new byte[]{(byte)0xcb});
                out.write(ByteBuffer.allocate(8).putDouble(num).array());
                break;
            }
            case TYPE_BOOL: {
                boolean b = (boolean)data;
                out.write(new byte[]{(byte)(b ? 0xc3: 0xc2)});
                break;
            }
            case TYPE_NONE: {
                out.write(new byte[]{(byte)0xc0});
                break;
            }
            case TYPE_NIL: {
                out.write(new byte[]{(byte)0xc0});
                break;
            }
            case TYPE_ARRAY: {
                ArrayList<PackData> arr = (ArrayList)data;
                int length = arr.size();
                if (length <= 0x0f) {
                    out.write(new byte[]{(byte)(0x90 + length)});
                } else if (length <= 0xffff){
                    out.write(new byte[]{(byte)0xdc});
                    packUint16(out, length);
                } else {
                    out.write(new byte[]{(byte)0xdd});
                    packUint32(out, length);
                }
                for (PackData data : arr) {
                    data.pack(out);
                }
                break;
            }
            case TYPE_BUFFER: {
                byte[] buf = (byte[])data;
                int length = buf.length;
                if (length <= 0x0f) {
                    out.write(new byte[]{(byte)(0xa0 + length)});
                } else if (length <= 0xffff){
                    out.write(new byte[]{(byte)0xda});
                    packUint16(out, length);
                } else {
                    out.write(new byte[]{(byte)0xdb});
                    packUint32(out, length);
                }
                out.write(buf);
                break;
            }
            case TYPE_MAP: {
                HashMap<String, PackData> map = (HashMap)data;
                Set<String> keys = map.keySet();
                int length = keys.size();
                if (length <= 0x0f) {
                    out.write(new byte[]{(byte)(0x80 + length)});
                } else if (length <= 0xffff){
                    out.write(new byte[]{(byte)0xde});
                    packUint16(out, length);
                } else {
                    out.write(new byte[]{(byte)0xdf});
                    packUint32(out, length);
                }
                for (String key : keys) {
                    packString(out, key);
                    map.get(key).pack(out);
                }
                break;
            }
        }
    }
}
