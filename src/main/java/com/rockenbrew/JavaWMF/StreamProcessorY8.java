package com.rockenbrew.JavaWMF;

import java.awt.*;
import java.awt.image.BufferedImage;

public class StreamProcessorY8 extends StreamProcessor {

	
	public synchronized BufferedImage processImage(byte[] imageByteArray, int w, int h)	{

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		//
		// y8 input stream
		//

		for (int i=0; i<w*h; i++) {

			int[] xy = GetCoordOfIndex(i, w);
			int x = xy[0];
			int y = xy[1];

			int r = imageByteArray[i] & 0xFF;
			int g = imageByteArray[i] & 0xFF;
			int b = imageByteArray[i] & 0xFF;

			try {
				image.setRGB(x, y, new Color(b,g,r).getRGB());
				
			} catch(IllegalArgumentException e) {
				image.setRGB(x, y, Color.RED.getRGB());
			}

		}
		
		BufferedImage finalImage = clone(image, image.getType());
				
		image.flush();
		
		return finalImage;
	}
	
	
}
