package com.gynt.lwproto.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.gynt.lwproto.LWProto;
import com.gynt.lwproto.LWProto.Serializer;
import com.gynt.lwproto.LWProto.lwproto;

public class Example {

	@lwproto
	private String name = "Hello world!";

	@lwproto
	public int age = 100;

	@lwproto
	public ArrayList<String> list = new ArrayList<>(Arrays.asList("a","b","c"));
	
	@lwproto
	public HashMap<String, Integer> map = new HashMap<String, Integer>();
	{
		map.put("example1", 1);
		map.put("example2", 2);
	}

	public static class AnotherExample {
		public int VERSION = 4;

		@lwproto(from = 1, until = 4)
		public String name = "Bye world!";

		@lwproto(from = 0)
		public int age = 90;
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoSuchFieldException, SecurityException {

		Serializer<Example> s = new Serializer<Example>(Example.class);

		Example e = new Example();
		System.out.println(s.deserialize(s.serialize(e)).name);
		System.out.println(s.deserialize(s.serialize(e)).list.toString());
		System.out.println(s.deserialize(s.serialize(e)).map.toString());

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
