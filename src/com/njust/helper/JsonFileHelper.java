package com.njust.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

public class JsonFileHelper
{
	public static boolean SavetoJson(ArrayList data, String outputfilename, String sd)
	{
		File outputfile = new File(outputfilename);
		if (!outputfile.exists())
		{
			try
			{
				outputfile.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		} else
		{

			try
			{
				outputfile.delete();
				outputfile.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		try
		{
			JsonGenerator generator = Json
					.createGenerator(new FileOutputStream(outputfile));
			generator.writeStartObject();

			generator.writeStartArray("PredictedData");

			for (int i = 0; i < data.size(); i++)
			{
				generator.writeStartObject();
				generator.write(sd, String.valueOf(data.get(i)));
				generator.writeEnd();
			}
			generator.writeEnd();
			generator.writeEnd();
			generator.close();
			return true;
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
