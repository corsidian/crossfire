/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 B5Chat Community. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b5chat.crossfire.nio;

/**
 * An Exception indicating that evaluated content is not valid XML.
 * 
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class XMLNotWellFormedException extends Exception {

	private static final long serialVersionUID = 1L;

	public XMLNotWellFormedException() {
		super();
	}

	public XMLNotWellFormedException(String message, Throwable cause) {
		super(message, cause);
	}

	public XMLNotWellFormedException(String message) {
		super(message);
	}

	public XMLNotWellFormedException(Throwable cause) {
		super(cause);
	}
}
