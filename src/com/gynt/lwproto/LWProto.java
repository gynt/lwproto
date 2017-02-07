package com.gynt.lwproto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

public abstract class LWProto {

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface lwproto {
		public int from() default 0;

		public int until() default Integer.MAX_VALUE;
	}

	public static abstract class AbstractSerializer<T> {

		protected Class<?> type;

		public AbstractSerializer(Class<?> c) {
			type = c;
		}

		public byte[] serializeField(Field f, T obj)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			if (map.containsKey(f.getType())) {

				@SuppressWarnings("unchecked")
				byte[] data = map.get(f.getType()).serialize(f.get(obj));
				return data;

			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
		};

		public void deserializeField(Field f, T obj, byte[] data)
				throws IllegalArgumentException, IllegalAccessException, InstantiationException {
			if (map.containsKey(f.getType())) {

				f.set(obj, map.get(f.getType()).deserialize(data));
				return;

			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
		};

		public abstract T deserialize(byte[] data) throws InstantiationException, IllegalAccessException;

		public abstract byte[] serialize(T obj)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException;

	}

	@SuppressWarnings("rawtypes")
	private static final HashMap<Class<?>, AbstractSerializer> map = new HashMap<>();

	static {
		map.put(String.class, new AbstractSerializer<String>(String.class) {

			@Override
			public String deserialize(byte[] data) {
				return new String(data, Charset.forName("UTF-8"));
			}

			@Override
			public byte[] serialize(String obj) {
				if(obj==null)obj="";
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
				return ByteBuffer.allocate(Integer.BYTES).putInt(obj).array();
			}

		});
		map.put(Integer.class, map.get(int.class));
		LWProto.register(long.class, new AbstractSerializer<Long>(long.class) {

			@Override
			public Long deserialize(byte[] data) {
				return ByteBuffer.wrap(data).getLong();
			}

			@Override
			public byte[] serialize(Long obj) {
				return ByteBuffer.allocate(Long.BYTES).putLong(obj).array();
			}

		});
		LWProto.register(Long.class, LWProto.retrieve(long.class));
		LWProto.register(double.class, new AbstractSerializer<Double>(double.class) {

			@Override
			public Double deserialize(byte[] data) throws InstantiationException, IllegalAccessException {
				return ByteBuffer.wrap(data).getDouble();
			}

			@Override
			public byte[] serialize(Double obj)
					throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
				return ByteBuffer.allocate(Double.BYTES).putDouble(obj).array();
			}

		});
		LWProto.register(Double.class, LWProto.retrieve(double.class));
		LWProto.register(float.class, new AbstractSerializer<Float>(float.class) {

			@Override
			public Float deserialize(byte[] data) throws InstantiationException, IllegalAccessException {
				return ByteBuffer.wrap(data).getFloat();
			}

			@Override
			public byte[] serialize(Float obj)
					throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
				return ByteBuffer.allocate(Float.BYTES).putFloat(obj).array();
			}

		});
		LWProto.register(Float.class, LWProto.retrieve(float.class));
		LWProto.register(byte.class, new AbstractSerializer<Byte>(byte.class) {

			@Override
			public Byte deserialize(byte[] data) throws InstantiationException, IllegalAccessException {
				return ByteBuffer.wrap(data).get();
			}

			@Override
			public byte[] serialize(Byte obj)
					throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
				return ByteBuffer.allocate(Byte.BYTES).put(obj).array();
			}

		});
		LWProto.register(Float.class, LWProto.retrieve(float.class));
	}

	@SuppressWarnings("unchecked")
	public static <T> AbstractSerializer<T> register(Class<T> type, AbstractSerializer<T> a) {
		return map.put(type, a);
	}

	@SuppressWarnings("unchecked")
	public static <T> AbstractSerializer<T> remove(Class<T> type) {
		return map.remove(type);
	}

	@SuppressWarnings("unchecked")
	public static <T> AbstractSerializer<T> retrieve(Class<T> type) {
		return map.get(type);
	}

	public static class Serializer<T> extends AbstractSerializer<T> {

		private boolean hasversion;
		private Field[] fields;

		public Serializer(Class<?> c) {
			super(c);

			hasversion = false;
			try {
				c.getDeclaredField("VERSION");
				hasversion = true;
			} catch (NoSuchFieldException e) {
				// e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}

			ArrayList<Field> temp = new ArrayList<>();
			for (Field f : c.getDeclaredFields()) {
				if (f.getDeclaredAnnotation(lwproto.class) == null)
					continue;
				f.setAccessible(true);
				temp.add(f);
			}
			fields = temp.toArray(new Field[0]);
		}

		public static <T> T[] toArray(Collection<T> list, Class<?> innertype) {
			@SuppressWarnings("unchecked")
			T[] toR = (T[]) java.lang.reflect.Array.newInstance(innertype, list.size());
			int i = 0;
			for (T o : list) {
				toR[i] = o;
				i++;
			}
			return toR;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public byte[] serializeField(Field f, T obj)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			if (Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				Class<?> innertype = (Class<?>) p.getActualTypeArguments()[0];
				return serializeArray(toArray((Collection) f.get(obj), innertype), innertype);
			} else if(Map.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				return serializeMap((Map) f.get(obj), (Class<?>) p.getActualTypeArguments()[0], (Class<?>) p.getActualTypeArguments()[1]);
			} else if (f.getType().isArray()) {

				return serializeArray(f.get(obj), f.getType().getComponentType());

			} else if (map.containsKey(f.getType())) {
				byte[] data = map.get(f.getType()).serialize(f.get(obj));
				return data;

			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void deserializeField(Field f, T obj, byte[] data)
				throws InstantiationException, IllegalAccessException {
			if (Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				Class<?> innertype = (Class<?>) p.getActualTypeArguments()[0];
				Collection list = (Collection) f.getType().newInstance();
				for(Object o: (Object[]) deserializeArray(data, innertype)) {
					list.add(o);
				}
				f.set(obj, list);
			} else if(Map.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				f.set(obj, deserializeMap(data, (Class<Map>) f.getType(), (Class<?>) p.getActualTypeArguments()[0], (Class<?>) p.getActualTypeArguments()[1]));
			} else if (f.getType().isArray()) {
				f.set(obj, deserializeArray(data, f.getType().getComponentType()));
			} else if (map.containsKey(f.getType())) {

				f.set(obj, map.get(f.getType()).deserialize(data));
				return;

			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
		}

		@Override
		public byte[] serialize(T obj)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			if (type.isArray())
				return serializeArray(obj);
			ArrayList<byte[]> datas = new ArrayList<>();

			int size = 0;
			int version = 0;
			if (hasversion) {
				version = type.getDeclaredField("VERSION").getInt(obj);
			}

			for (Field f : fields) {
				lwproto anno = f.getDeclaredAnnotation(lwproto.class);
				if (version < anno.from() || version > anno.until())
					continue;

				byte[] data = serializeField(f, obj);
				datas.add(data);
				size += data.length;
			}

			if (size == 0)
				throw new RuntimeException("Nothing has been serialized");

			ByteBuffer b = ByteBuffer.allocate(4 + size + (4 * datas.size()));
			b.putInt(version);
			for (byte[] data : datas) {
				b.putInt(data.length);
				b.put(data);
			}

			return b.array();

		}

		@Override
		public T deserialize(byte[] data) throws InstantiationException, IllegalAccessException {

			if (type.isArray())
				return deserializeArray(data);
			@SuppressWarnings("unchecked")
			T t = (T) type.newInstance();

			Queue<byte[]> datas = new LinkedList<>();
			ByteBuffer b = ByteBuffer.wrap(data);
			int version = b.getInt();
			while (b.hasRemaining()) {
				int size = b.getInt();
				byte[] d = new byte[size];
				b.get(d);
				datas.offer(d);
			}

			for (Field f : fields) {
				lwproto anno = f.getDeclaredAnnotation(lwproto.class);
				if (version < anno.from() || version > anno.until())
					continue;
				deserializeField(f, t, datas.poll());
			}

			return t;

		}

		public static <K,V> byte[] serializeMap(Map<K,V> obj, Class<K> componenttype1, Class<V> componenttype2) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoSuchFieldException, SecurityException {
			if (!map.containsKey(componenttype1)) {
				throw new RuntimeException("Unsupported class: " + componenttype1.getName());
			} else if(!map.containsKey(componenttype2)) {
				throw new RuntimeException("Unsupported class: " + componenttype2.getName());
			}
			int length = obj.size();
			int size = 0;
			ArrayList<byte[]> datas = new ArrayList<byte[]>();
			for(Entry<K,V> entry : obj.entrySet()) {
				@SuppressWarnings("unchecked")
				byte[] data1 = map.get(componenttype1).serialize(entry.getKey());
				@SuppressWarnings("unchecked")
				byte[] data2 = map.get(componenttype2).serialize(entry.getValue());
				size+=data1.length+data2.length;
				datas.add(data1);
				datas.add(data2);
			}
			ByteBuffer b = ByteBuffer.allocate(4+size+(4*datas.size()));
			b.putInt(length);
			for (byte[] data : datas) {
				b.putInt(data.length);
				b.put(data);
			}
			return b.array();
		}

		public static <K,V> Map<K, V> deserializeMap(byte[] data, @SuppressWarnings("rawtypes") Class<Map> class1, Class<?> class2, Class<?> class3) throws InstantiationException, IllegalAccessException {
			if (!map.containsKey(class2)) {
				throw new RuntimeException("Unsupported class: " + class2.getName());
			} else if(!map.containsKey(class3)) {
				throw new RuntimeException("Unsupported class: " + class3.getName());
			}
			ByteBuffer b = ByteBuffer.wrap(data);
			int length = b.getInt(); //Amount of entries
			ArrayList<Entry<K,V>> entries = new ArrayList<>();
			int i = 0;
			while (i < length) {

				byte[] c1 = new byte[b.getInt()];
				b.get(c1);
				byte[] c2 = new byte[b.getInt()];
				b.get(c2);
				entries.add(new Entry<K, V>() {

					@SuppressWarnings("unchecked")
					K key = (K) map.get(class2).deserialize(c1);
					@SuppressWarnings("unchecked")
					V value = (V) map.get(class3).deserialize(c2);

					@Override
					public K getKey() {
						return key;
					}

					@Override
					public V getValue() {
						return value;
					}

					@Override
					public V setValue(V value) {
						return null;
					}
				});

				i++;
			}
			@SuppressWarnings("unchecked")
			Map<K,V> m = class1.newInstance();
			for(Entry<K,V> entry : entries) {
				m.put(entry.getKey(), entry.getValue());
			}
			return m;
		}

		public static <T> byte[] serializeArray(T obj, Class<?> componenttype)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {
			ArrayList<byte[]> datas = new ArrayList<>();

			int size = 0;

			int length = Array.getLength(obj);
			for (int i = 0; i < length; i++) {
				Object arrayElement = Array.get(obj, i);
				if (map.containsKey(componenttype)) {
					@SuppressWarnings("unchecked")
					byte[] data = map.get(componenttype).serialize(arrayElement);
					datas.add(data);
					size += data.length;
				} else {
					throw new RuntimeException("Unsupported class: " + componenttype.getName());
				}
			}

			ByteBuffer b = ByteBuffer.allocate(4 + size + (4 * datas.size()));
			b.putInt(length);
			for (byte[] data : datas) {
				b.putInt(data.length);
				b.put(data);
			}

			return b.array();
		}

		protected byte[] serializeArray(T obj)
				throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException {

			return serializeArray(obj, type.getComponentType());

		}

		public static <T> T deserializeArray(byte[] data, Class<T> componenttype) throws ArrayIndexOutOfBoundsException,
				IllegalArgumentException, InstantiationException, IllegalAccessException {
			ByteBuffer b = ByteBuffer.wrap(data);
			int length = b.getInt();
			@SuppressWarnings("unchecked")
			T t = (T) Array.newInstance(componenttype, length);

			int i = 0;
			while (i < length) {
				if (map.containsKey(componenttype)) {
					byte[] d = new byte[b.getInt()];
					b.get(d);
					Array.set(t, i, map.get(componenttype).deserialize(d));
				} else {
					throw new RuntimeException("Unsupported class: " + componenttype.getName());
				}
				i++;
			}

			return t;
		}

		@SuppressWarnings("unchecked")
		protected T deserializeArray(byte[] data)
				throws ArrayIndexOutOfBoundsException, InstantiationException, IllegalAccessException {

			return (T) deserializeArray(data, type.getComponentType());

		}

	}
}