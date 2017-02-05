package com.gynt.lwproto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractSerializer<T> {

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface lwproto {
		public int from() default 0;
		public int until() default Integer.MAX_VALUE;
	}



	protected Class<?> type;

	public AbstractSerializer(Class<?> c) {
		type=c;
	}


	public abstract T deserialize(byte[] data);
	public abstract byte[] serialize(T obj);

	public static class Serializer<T> extends AbstractSerializer<T> {

	public static final HashMap<Class<?>, AbstractSerializer> map = new HashMap<>();

	static {
		map.put(String.class, new AbstractSerializer<String>(String.class) {

			@Override
			public String deserialize(byte[] data) {
				return new String(data, Charset.forName("UTF-8"));
			}

			@Override
			public byte[] serialize(String obj) {
				return obj.getBytes(Charset.forName("UTF-8"));
			}
		});
		map.put(int.class, new AbstractSerializer<Integer>(int.class) {

			@Override
			public Integer deserialize(byte[] data) {
				return ByteBuffer.wrap(data).getInt();
			}

			@Override
			public byte[] serialize(Integer obj) {
				return ByteBuffer.allocate(4).putInt(obj).array();
			}

		});
		map.put(Integer.class, map.get(int.class));

	}


	private boolean hasversion;

	public Serializer(Class<?> c) {
		super(c);

		hasversion=false;
		try {
			c.getDeclaredField("VERSION");
			hasversion=true;
		} catch (NoSuchFieldException e) {
			//e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	public byte[] serialize(T obj) {
		try {

			ArrayList<byte[]> datas = new ArrayList<>();

			int size = 0;
			int version = 0;
			if(hasversion) {
				version = type.getDeclaredField("VERSION").getInt(obj);
			}

			for (Field f : type.getDeclaredFields()) {
				lwproto anno = f.getDeclaredAnnotation(lwproto.class);
				if(anno==null) {
					System.out.println("c");
					continue;
				}
				if(version < anno.from() || version > anno.until()) {
					System.out.println("a");
					continue;
				}
				if (map.containsKey(f.getType())) {
					byte[] data = map.get(f.getType()).serialize(f.get(obj));
					datas.add(data);
					size += data.length;
				} else {
					throw new RuntimeException("Unsupported class: " + f.getType().getName());
				}
			}

			if (size == 0)
				throw new RuntimeException("Nothing has been serialized");

			ByteBuffer b = ByteBuffer.allocate(4+size + (4 * datas.size()));
			b.putInt(version);
			for (byte[] data : datas) {
				b.putInt(data.length);
				b.put(data);
			}

			return b.array();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public T deserialize(byte[] data) {
		try {
			T t = (T) type.newInstance();

			Queue<byte[]> datas = new LinkedList<>();
			ByteBuffer b = ByteBuffer.wrap(data);
			int version = b.getInt();
			while(b.hasRemaining()) {
				int size = b.getInt();
				byte[] d = new byte[size];
				b.get(d);
				datas.offer(d);
			}

			for(Field f : type.getDeclaredFields()) {
				lwproto anno = f.getDeclaredAnnotation(lwproto.class);
				if(anno==null) continue;
				if(version < anno.from() || version > anno.until()) continue;
				if(map.containsKey(f.getType())) {
					f.set(t, map.get(f.getType()).deserialize(datas.poll()));
				} else {
					throw new RuntimeException("Unsupported class: " + f.getType().getName());
				}
			}

			return t;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	}

}
