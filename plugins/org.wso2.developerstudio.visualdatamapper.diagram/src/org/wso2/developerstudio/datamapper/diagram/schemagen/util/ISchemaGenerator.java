package org.wso2.developerstudio.datamapper.diagram.schemagen.util;

import java.io.IOException;


public interface ISchemaGenerator {

	public abstract String getSchemaResourcePath(String filePath) throws IOException;

	public abstract String getSchemaContent(String fileText) throws IOException;

}