package com.gynt.lwproto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.gynt.lwproto.LWProto.AbstractSerializer;

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

		public byte[] serializeField(Field f, T obj) {
			if (map.containsKey(f.getType())) {
				try {
					@SuppressWarnings("unchecked")
					byte[] data = map.get(f.getType()).serialize(f.get(obj));
					return data;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
			return null;
		};

		public void deserializeField(Field f, T obj, byte[] data) {
			if (map.containsKey(f.getType())) {
				try {
					f.set(obj, map.get(f.getType()).deserialize(data));
					return;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
		};

		public abstract T deserialize(byte[] data);

		public abstract byte[] serialize(T obj);

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
		map.put(String[].class, new Serializer<String[]>(String[].class));
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

		@Override
		public byte[] serializeField(Field f, T obj) {
			if(Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				Class<?> innertype = (Class<?>) p.getActualTypeArguments()[0];
				if (map.containsKey(Array.newInstance(innertype, 0).getClass())) {
					try {
						return map.get(Array.newInstance(innertype, 0).getClass()).serialize(((Collection) f.get(obj)).toArray());
					} catch (NegativeArraySizeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					throw new RuntimeException(
							"Unsupported class: " + Array.newInstance((Class<?>) p.getActualTypeArguments()[0], 0).getClass().getName());
				}
			} else if (map.containsKey(f.getType())) {
				try {
					@SuppressWarnings("unchecked")
					byte[] data = map.get(f.getType()).serialize(f.get(obj));
					return data;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
			return null;
		}


		@Override
		public void deserializeField(Field f, T obj, byte[] data) {
			if(Collection.class.isAssignableFrom(f.getType())) {
				ParameterizedType p = (ParameterizedType) f.getGenericType();
				Class<?> innertype = (Class<?>) p.getActualTypeArguments()[0];
				if (map.containsKey(Array.newInstance(innertype, 0).getClass())) {

					try {
						List l = (List) f.getType().newInstance();
						for (Object o : (Object[]) map.get(Array.newInstance(innertype, 0).getClass()).deserialize(data)) {
							l.add(o);
						}
						f.set(obj, l);
						return;
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {
					throw new RuntimeException(
							"Unsupported class: " + Array.newInstance(innertype, 0).getClass().getClass().getName());
				}
			} else if (map.containsKey(f.getType())) {
				try {
					f.set(obj, map.get(f.getType()).deserialize(data));
					return;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				throw new RuntimeException("Unsupported class: " + f.getType().getName());
			}
			throw new RuntimeException("Something failed for: " + f.getType().getName());
		}

		@Override
		public byte[] serialize(T obj) {

			try {
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

					byte[] data = serializeField(f,obj);
					datas.add(data);
					size+=data.length;
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
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected byte[] serializeArray(T obj) {
			try {
				ArrayList<byte[]> datas = new ArrayList<>();

				int size = 0;

				int length = Array.getLength(obj);
				for (int i = 0; i < length; i++) {
					Object arrayElement = Array.get(obj, i);
					if (map.containsKey(type.getComponentType())) {
						@SuppressWarnings("unchecked")
						byte[] data = map.get(type.getComponentType()).serialize(arrayElement);
						datas.add(data);
						size += data.length;
					} else {
						throw new RuntimeException("Unsupported class: " + type.getComponentType().getName());
					}
				}

				ByteBuffer b = ByteBuffer.allocate(4 + size + (4 * datas.size()));
				b.putInt(length);
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

		@Override
		public T deserialize(byte[] data) {
			try {
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

		protected T deserializeArray(byte[] data) {
			try {
				ByteBuffer b = ByteBuffer.wrap(data);
				int length = b.getInt();
				@SuppressWarnings("unchecked")
				T t = (T) Array.newInstance(type.getComponentType(), length);

				int i = 0;
				while (i < length) {
					if (map.containsKey(type.getComponentType())) {
						byte[] d = new byte[b.getInt()];
						b.get(d);
						Array.set(t, i, map.get(type.getComponentType()).deserialize(d));
					} else {
						throw new RuntimeException("Unsupported class: " + type.getComponentType().getName());
					}
					i++;
				}

				return t;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	public static class CollectionSerializer<T extends List> extends Serializer<T> {

		private Class<?> innertype;

		public CollectionSerializer(Class<? extends List> c, Class<?> t) {
			super(c);
			innertype = t;
		}

		@Override
		public byte[] serialize(T obj) {
			if (map.containsKey(Array.newInstance(innertype, 0).getClass())) {
				return map.get(Array.newInstance(innertype, 0).getClass()).serialize(obj.toArray());
			} else {
				throw new RuntimeException(
						"Unsupported class: " + Array.newInstance(innertype, 0).getClass().getName());
			}
		}

		@Override
		public T deserialize(byte[] data) {
			try {
				if (map.containsKey(Array.newInstance(innertype, 0).getClass())) {
					List l = (List) type.newInstance();
					for (Object o : (Object[]) map.get(Array.newInstance(innertype, 0).getClass()).deserialize(data)) {
						l.add(o);
					}
					return (T) l;
				} else {
					throw new RuntimeException(
							"Unsupported class: " + Array.newInstance(innertype, 0).getClass().getClass().getName());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

	}
}