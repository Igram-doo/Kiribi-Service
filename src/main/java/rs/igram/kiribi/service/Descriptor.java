/* 
 * MIT License
 * 
 * Copyright (c) 2020 Igram, d.o.o.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
package rs.igram.kiribi.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import rs.igram.kiribi.service.util.ConsumerSupport;
import rs.igram.kiribi.io.Encodable;
import rs.igram.kiribi.io.VarInput;
import rs.igram.kiribi.io.VarInputStream;
import rs.igram.kiribi.io.VarOutput;

/**
 * An instance of this class describes a service.
 *
 * @author Michael Sargent
 */
public final class Descriptor implements Encodable {
	private static final int VERSION_0 = 0;

	private final int version;
	private final ServiceAddress address;
	private final int type;
	private final Scope scope;
	private final ConsumerSupport<Description> descriptionSupport = new ConsumerSupport<>();
		
	private Description description = new Description();
	private String tags = "";
	private String group = "";
	// extensions for fututre releases
	private byte[] ext = new byte[0];

	/**
	 * Initializes a newly created <code>Descriptor</code> object
	 * with the given arguments.
	 *
	 * @param address The address of the service.
	 * @param group The group of the service.
	 * @param type The type of the service.
	 */
	public Descriptor(ServiceAddress address, String group, int type) {
		this(address, type, Scope.PUBLIC, group, "", new Description());
	}
	
	/**
	 * Initializes a newly created <code>Descriptor</code> object
	 * with given arguments.
	 *
	 * @param address The address of the service.
	 * @param type The type of the service.
	 * @param scope The scope of the service.
	 * @param group The group of the service.
	 * @param tags The tags of the service.
	 * @param description The description of the service.
	 */
	public Descriptor(ServiceAddress address, int type, Scope scope, String group, String tags, Description description) {
		version = VERSION_0;
		this.address = address;
		this.type = type;	
		this.scope = scope;
		this.description = description;
		this.group = group;
		this.tags = tags;
	}

	/**
	 * Initializes a newly created <code>Descriptor</code> object
	 * with the given input stream.
	 *
	 * @param in The input stream to initialize from.
	 * @throws IOException if there was a probem reading from the input stream.
	 */
	public Descriptor(VarInput in) throws IOException {
		version = in.readUnsignedByte();
		address = new ServiceAddress(in);
		type = in.readInt();
		scope = in.readEnum(Scope.class);
		group = in.readUTF();
		tags = in.readUTF();
		description = new Description(in);
		// extensions
		ext = in.readBytes();
	}

	/**
	 * Initializes a newly created <code>Descriptor</code> object
	 * with the given byte array.
	 *
	 * @param b The byte array to initialize from.
	 * @throws IOException if there was a probem reading from the byte array.
	 */
	Descriptor(byte[] b) throws IOException {
		this(new VarInputStream(b));
	}

	@Override
	public void write(VarOutput out) throws IOException {
		out.writeByte(version);
		address.write(out);
		out.writeInt(type);
		out.writeEnum(scope);
		out.writeUTF(group);
		out.writeUTF(tags);
		out.write(description);
		// extensions
		out.writeBytes(ext);
	}

	/**
	 * Returns <code>true</code> if the scope of the service is public,
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if the scope of the service is public,
	 * <code>false</code> otherwise.
	 */
	public boolean isPublic() {return scope == Scope.PUBLIC;}
	
	/**
	 * Returns the version of the service.
	 *
	 * @return The version of the service.
	 */
	public int getVersion() {return version;}
	
	/**
	 * Returns the service address of the service.
	 *
	 * @return The service address of the service.
	 */
	public ServiceAddress getAddress() {return address;}
	
	/**
	 * Returns the type of the service.
	 *
	 * @return The type of the service.
	 */
	public int getType() {return type;}
	
	/**
	 * Returns the scope of the service.
	 *
	 * @return The scope of the service.
	 */
	public Scope getScope() {return scope;}
	
	/**
	 * Sets the description of the service.
	 *
	 * @param value The description of the service.
	 */
	public void setDescription(Description value) {
		description = value;
		descriptionSupport.consume(description);
	}
	
	/**
	 * Sets the group of the service.
	 *
	 * @param value The group of the service.
	 */
	public void setGroup(String value) {group = value;}
	
	/**
	 * Sets the tags of the service.
	 *
	 * @param value The tags of the service.
	 */
	public void setTags(String value) {tags = value;}
	
	/**
	 * Returns the description of the service.
	 *
	 * @return The description of the service.
	 */
	public Description getDescription() {return description;}
	
	/**
	 * Returns the group of the service.
	 *
	 * @return The group of the service.
	 */
	public String getGroup() {return group;}
	
	/**
	 * Returns the tags of the service.
	 *
	 * @return The tags of the service.
	 */
	public String getTags() {return tags;}

	void addDescriptionConsumer(Consumer<Description> consumer) {descriptionSupport.addConsumer(consumer);}
	void removeDescriptionConsumer(Consumer<Description> consumer) {descriptionSupport.removeConsumer(consumer);}
	
	/**
	 * Returns the set of tags of the service.
	 *
	 * @return The set of tags of the service.
	 */
	public Set<String> tags(){
		return parseTags(getTags());
	}
	
	static Set<String> parseTags(String tags){
		var result = new HashSet<String>();
		if(!tags.isEmpty()) {
			var items = tags.toLowerCase().split("\\s");
			var L = Math.min(5, items.length);
			for(int i = 0; i < L; i++) result.add(items[i]);
		}
		return result;
	}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(o != null && o.getClass() == Descriptor.class) {
			var d = (Descriptor)o;
			return version == d.version
			       && address.equals(d.address)
			       && type == d.type
			       && getScope() == d.getScope()
			       && group.equals(d.group)
			       && tags.equals(d.tags)
			       && description.equals(d.description);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return address.hashCode();
	}

	@Override
	public String toString() {
		return "Descriptor:["+Arrays.deepToString(
		           new Object[]{version, address, type, getScope(), group, tags, description}
		       )+"]";
	}

	/**
	 * An instance of this class provides a short description of a service.
	 *
	 * @author Michael Sargent
	 */
	public static final class Description implements Encodable {
		private final String title;
		private final String description;
		private final byte[] image;

		/**
	  	 * Initializes a newly created <code>Description</code> object.
	  	 */
		public Description() {
			this("", "");
		}
		
		/**
	  	 * Initializes a newly created <code>Descriptor</code> object
	  	 * with the given title.
	  	 *
	  	 * @param title The title of the service.
	  	 */
		public Description(String title) {
			this(title, "");
		}
		
		/**
	  	 * Initializes a newly created <code>Description</code> object
	  	 * with the given title and description.
	  	 *
	  	 * @param title The title of the service.
	  	 * @param description A short description of the service.
	  	 */
		public Description(String title, String description) {
			this(title, description, new byte[0]);
		}

		/**
	  	 * Initializes a newly created <code>Description</code> object
	  	 * with the given title, description and byte array.
	  	 *
	  	 * @param title The title of the service.
	  	 * @param description A short description of the service.
	  	 * @param im A byte array containing image data of the service.
	  	 */
		public Description(String title, String description, byte[] im) {
			if(title.toLowerCase().contains("kiribi")) 
				throw new IllegalArgumentException("\"Kiribi\" is a reserved keyword");
			this.title = crop(title, 15);
			this.description = crop(description, 140);

			if(im == null) {
				image = new byte[0];
			} else {
				final int L = im.length;
				image = new byte[L];
				System.arraycopy(im, 0, image, 0, L);
			}
		}

		/**
	  	 * Initializes a newly created <code>Description</code> object
	  	 * with the provided input stream.
	  	 *
	  	 * @param in The input stream to initialize from.
	  	 * @throws IOException if there was a probem reading from the input stream.
	  	 */
		public Description(VarInput in) throws IOException {
			title = in.readUTF();
			description = in.readUTF();
			image = in.readBytes();
		}

		/**
	  	 * Initializes a newly created <code>Description</code> object
	  	 * with a byte array.
	  	 *
	  	 * @param b The byte array to initialize from.
	  	 * @throws IOException if there was a probem reading from the byte array.
	  	 */
		Description(byte[] b) throws IOException {
			this(new VarInputStream(b));
		}

		@Override
		public void write(VarOutput out) throws IOException {
			out.writeUTF(getTitle());
			out.writeUTF(getDescription());
			out.writeBytes(image);
		}

		/**
		 * Returns the title of the service.
		 *
		 * @return The title of the service.
		 */
		public String getTitle() {return title;}
		
		/**
		 * Returns the description of the service.
		 *
		 * @return The description of the service.
		 */
		public String getDescription() {return description; }
		
		/**
		 * Returns the byte array containing an image of the service.
		 *
		 * @return The byte array containing an image of the service.
		 */
		public byte[] getImage() {
			final var L = image.length;
			final var result  = new byte[L];
			System.arraycopy(image, 0, result, 0, L);
			return result;
		}

		@Override
		public boolean equals(Object o){
			if(o != null && o.getClass().equals(Description.class)) {
				var d = (Description)o;
				return title.equals(d.title)
				       && description.equals(d.description)
				       && Arrays.equals(image, d.image);
			}
			return false;
		}

		@Override
		public String toString() {
			return "Description:[title="+title+",description="+description+"]";
		}
		
		private static String crop(String src, int length) {
			return src.length() > length ? src.substring(0, length) : src;
		}
	}
}
