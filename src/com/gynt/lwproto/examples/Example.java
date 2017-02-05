package com.gynt.lwproto.examples;

import com.gynt.lwproto.LWProto;
import com.gynt.lwproto.LWProto.Serializer;
import com.gynt.lwproto.LWProto.lwproto;

public class Example {

	@lwproto
	private String name = "Hello world!";

	@lwproto
	public int age = 100;

	public static class AnotherExample {
		public int VERSION = 4;

		@lwproto(from = 1, until = 4)
		public String name = "Bye world!";

		@lwproto(from = 0)
		public int age = 90;
	}

	public static void main(String[] args) {
		Serializer<String[]> ss = new Serializer<String[]>(String[].class);

		String[] sss = new String[] { "a", "bc", "def" };
		ss.serialize(sss);
		System.out.println(ss.deserialize(ss.serialize(sss)).length);

		Serializer<Example> s = new Serializer<Example>(Example.class);

		Example e = new Example();

		System.out.println(new String(s.serialize(e)));
		System.out.println(s.deserialize(s.serialize(e)).name);

		LWProto.register(Example.class, s);
		Serializer<Example[]> q = new Serializer<Example[]>(Example[].class);
		Example e1 = new Example();
		Example e2 = new Example();
		e1.name = "foo";
		e2.name = "bar";
		Example[] es = new Example[] { e1, e2 };
		System.out.println(q.deserialize(q.serialize(es)).length);
	}

}
