package com.gynt.lwproto;

import com.gynt.lwproto.AbstractSerializer.lwproto;

public class Example {

	@lwproto
	public String name = "Hello world!";

	@lwproto
	public int age = 100;

	public static class AnotherExample {
		public int VERSION=4;

		@lwproto(from=1, until=4)
		public String name = "Bye world!";

		@lwproto(from=0)
		public int age = 90;
	}

	public static void main(String[] args) {
		AbstractSerializer.Serializer<Example> s = new AbstractSerializer.Serializer<Example>(Example.class);

		Example e = new Example();

		System.out.println(new String(s.serialize(e)));
		System.out.println(s.deserialize(s.serialize(e)).name);

	}

}
