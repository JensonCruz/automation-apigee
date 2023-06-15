package com.optus.apigee.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ServiceAutomation {
	public static void main(String[] args) throws IOException {
		// Step 1: Load properties from application.properties file
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties properties = new Properties();
		try (InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
			properties.load(resourceStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String excelFilePath;
		String folderBasePath;
		String manageServicePath;
		

//		if (args.length < 2) {
//			System.out
//					.println("Please provide the 'excel.file.path' and 'folder.base.path' as command-line arguments.");
//			return;
//		}

		if (args.length >= 2) {
			excelFilePath = args[0];
			folderBasePath = args[1];
			manageServicePath = args[2];
			System.out.println("Cmd line of excel path: " + excelFilePath);
			System.out.println("Cmd line of folderBasePath: " + folderBasePath);
		} else {

			excelFilePath = properties.getProperty("excel.file.path");
			folderBasePath = properties.getProperty("folder.base.path");
			manageServicePath = properties.getProperty("manage.service.path");
		}

		System.out.println("Read application properties");

		// Read Excel file

		FileInputStream inputStream = null;
		Workbook workbook = null;
		String password = null;
		try {
			inputStream = new FileInputStream(excelFilePath);
			workbook = new XSSFWorkbook(inputStream);

			// Rest of your code here

			Sheet sheet = workbook.getSheetAt(0);

			// Step 2: Get column indexes based on headers
			int servicenameColumnIndex = getColumnIndex(sheet, "Service Name");
			int contextPathColumnIndex = getColumnIndex(sheet, "Context Path");
			int serverNameColumnIndex = getColumnIndex(sheet, "Target Server Name");
			int actionColumnIndex = getColumnIndex(sheet, "Action");

			System.out.println("Read excel header");

			// Iterate through rows
			Iterator<Row> rowIterator = sheet.iterator();
			rowIterator.next();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				Cell actionCell = row.getCell(actionColumnIndex);
				String action = actionCell.getStringCellValue();

				Cell servicenameCell = row.getCell(servicenameColumnIndex);
				String serviceName = servicenameCell.getStringCellValue();

				Cell contextPathCell = row.getCell(contextPathColumnIndex);
				String contexPath = contextPathCell.getStringCellValue();

				Cell serverNameCell = row.getCell(serverNameColumnIndex);
				String serverName = serverNameCell.getStringCellValue();

				if (action.equalsIgnoreCase("Skip")) {
					System.out.println("skipping the value for the serviceName  : " + serviceName);
					continue;
				}

				// Step 3: Create a new folder using the "Service Name" header value
				System.out.println("Value for the serviceName  : " + serviceName);
				File newFolder = createNewFolder(serviceName, folderBasePath);

				// Step 4: Rename the newly created folder to the "Service Name" header value
				renameFolder(newFolder, serviceName);

				// Step 5: Copy the contents of the source folder to the newly created folder
				System.out.println("Folder base path  : " + newFolder.getPath());
				copyFolderContents(manageServicePath, newFolder.getPath());

				// Step 6: Rename xml file name
				File xmlFilePath = renameFileContents(newFolder.getPath(), serviceName);

				// Step 7: Change service xml file content
				changeServiceXmlContents(newFolder.getPath(), serviceName, contexPath);

				// Step 8: Change proxy xml file content
				changeProxyXmlContents(newFolder.getPath(), contexPath);

				// Step 9: Change proxy xml file content
				changeTargetServerContent(newFolder.getPath(), serverName);

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				workbook.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}

	private static void changeServiceXmlContents(String xmlFilePath, String serviceName, String contextPath)
			throws IOException {

		String filePath = xmlFilePath + "/apiproxy/" + serviceName + ".xml";

		try {
			String xmlContent = new String(Files.readAllBytes(Paths.get(filePath)));

			// Update the XML content
			String updatedXmlContent = xmlContent
					.replace("name=\"manage_campaign_service\"", "name=\"" + serviceName + "\"")
					.replace("<DisplayName>manage_campaign_service</DisplayName>",
							"<DisplayName>" + serviceName + "</DisplayName>")
					.replace("<Description>manage_campaign_service</Description>",
							"<Description>" + serviceName + "</Description>")
					.replace(
							"<BasePaths>/sdpcoreesb/campaignandfunnelmanagement/campaignmanagement/ManageCampaign</BasePaths>",
							"<BasePaths>" + contextPath + "</BasePaths>");

			// Write the updated XML content back to the file
			Files.write(Paths.get(filePath), updatedXmlContent.getBytes());
			System.out.println("Service XML file content updated successfully.");
		} catch (IOException e) {
			System.out.println("Error reading or writing the Service XML file.");
			e.printStackTrace();
		}

	}

	private static void changeProxyXmlContents(String folderPath, String contextPath) {
		String proxyFilePath = folderPath + "/apiproxy/proxies/default.xml";

		try {
			String xmlContent = new String(Files.readAllBytes(Paths.get(proxyFilePath)));

			// Update the XML content
			String updatedXmlContent = xmlContent

					.replace(
							"<BasePath>/sdpcoreesb/campaignandfunnelmanagement/campaignmanagement/ManageCampaign</BasePath>",
							"<BasePath>" + contextPath + "</BasePath>");

			// Write the updated XML content back to the file
			Files.write(Paths.get(proxyFilePath), updatedXmlContent.getBytes());
			System.out.println("Proxy XML file content updated successfully.");
		} catch (IOException e) {
			System.out.println("Error reading or writing the Proxy XML file.");
			e.printStackTrace();
		}

	}

	private static void changeTargetServerContent(String folderPath, String serverName) {
		String targetFilePath = folderPath + "/apiproxy/targets/default.xml";

		try {
			String xmlContent = new String(Files.readAllBytes(Paths.get(targetFilePath)));

			// Update the XML content
			String updatedXmlContent = xmlContent

					.replace("<Server name=\"ts-ocp-gnp4\"/>", "<Server name=\"" + serverName + "\"/>");

			// Write the updated XML content back to the file
			Files.write(Paths.get(targetFilePath), updatedXmlContent.getBytes());
			System.out.println("Proxy XML file content updated successfully.");
		} catch (IOException e) {
			System.out.println("Error reading or writing the Proxy XML file.");
			e.printStackTrace();
		}

	}

	private static File renameFileContents(String folderPath, String serviceName) {
		// Navigate to the "apiproxy" folder
		File apiproxyFolder = new File(folderPath, "apiproxy");

		// Get the file to be renamed
		File xmlFile = new File(apiproxyFolder, "manage_campaign_service.xml");

		// Check if the file exists
		if (xmlFile.exists()) {
			// Generate the new file name with the service name
			String newFileName = serviceName + ".xml";

			// Create a new file object with the desired name
			File newXmlFile = new File(apiproxyFolder, newFileName);

			// Rename the file
			if (xmlFile.renameTo(newXmlFile)) {
				System.out.println("File renamed successfully to: " + newFileName);
			} else {
				System.out.println("Failed to rename file.");
			}
		} else {
			System.out.println("File does not exist.");
		}

		return xmlFile;

	}

	private static File createNewFolder(String folderName, String folderBasePath) {
		System.out.println("Going to create new folder");
		File newFolder = new File(folderBasePath, folderName);
		if (!newFolder.exists()) {
			System.out.println("Folder created");
			newFolder.mkdir();
		}
		return newFolder;
	}

	private static void copyFolderContents(String sourceFolderPath, String destinationFolderPath) {
		System.out.println("Going to copyFolderContents");
		File sourceFolder = new File(sourceFolderPath);
		File destinationFolder = new File(destinationFolderPath);
		try {
			FileUtils.copyDirectory(sourceFolder, destinationFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void renameFolder(File folder, String newFolderName) {
		File newFolder = new File(folder.getParentFile(), newFolderName);
		folder.renameTo(newFolder);
	}

	private static int getColumnIndex(Sheet sheet, String header) {
		Row headerRow = sheet.getRow(0);
		Iterator<Cell> cellIterator = headerRow.cellIterator();
		while (cellIterator.hasNext()) {
			Cell cell = cellIterator.next();
			if (cell.getStringCellValue().equalsIgnoreCase(header)) {
				return cell.getColumnIndex();
			}
		}
		return -1;
	}

}