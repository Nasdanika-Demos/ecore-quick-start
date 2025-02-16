package org.nasdanika.demos.diagrams.mapping.tests;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.capability.emf.ResourceSetRequirement;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.excel.CellRow;
import org.nasdanika.models.excel.ExcelFactory;
import org.nasdanika.models.excel.RowSheet;
import org.nasdanika.models.excel.Workbook;
import org.nasdanika.models.family.FamilyFactory;
import org.nasdanika.models.family.FamilyPackage;
import org.nasdanika.models.family.Man;
import org.nasdanika.models.family.Person;
import org.nasdanika.models.family.Polity;
import org.nasdanika.models.family.Woman;
import org.nasdanika.ncore.util.NcoreResourceSet;

public class TestEcoreQuickStart {
	
	private void createResource() throws IOException {
		// Creating a model
		FamilyFactory familyFactory = FamilyFactory.eINSTANCE;
		
		Polity usa = familyFactory.createPolity();
		usa.setName("USA");
		
		Polity florida = familyFactory.createPolity();
		florida.setName("Florida");
		usa.getConstituents().add(florida);
		
		Woman jane = familyFactory.createWoman();
		jane.setName("Jane");
		florida.getResidents().add(jane);
		
		Polity texas = familyFactory.createPolity();
		texas.setName("Texas");
		usa.getConstituents().add(texas);
		
		Man joe = familyFactory.createMan();
		joe.setName("Joe");
		texas.getResidents().add(joe);
		jane.setFather(joe);				
		
		// Saving to a file
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet
			.getResourceFactoryRegistry()
			.getExtensionToFactoryMap()
			.put(
				Resource.Factory.Registry.DEFAULT_EXTENSION, 
				new	XMIResourceFactoryImpl());		
		URI resourceURI = URI.createFileURI(new File("target/family.xmi").getCanonicalPath());
		Resource resource = resourceSet.createResource(resourceURI);
		resource.getContents().add(usa);
		resource.save(null);		
	}
	
	private void readResource() throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet
			.getResourceFactoryRegistry()
			.getExtensionToFactoryMap()
			.put(
				Resource.Factory.Registry.DEFAULT_EXTENSION, 
				new XMIResourceFactoryImpl());
		
		resourceSet
			.getPackageRegistry()
			.put(
				FamilyPackage.eNS_URI, 
				FamilyPackage.eINSTANCE);
		
		URI resourceURI = URI.createFileURI(new File("target/family.xmi").getCanonicalPath());
		Resource resource = resourceSet.getResource(resourceURI, true);
		Polity usa = (Polity) resource.getContents().get(0);
		
		System.out.println(usa.getName());
		System.out.println(usa.getConstituents().size());		
	}

	/**
	 * Creates a family model programmatically, saves to XMI,
	 * loads from XMI
	 * @throws IOException
	 */
	@Test
	public void testSimple() throws IOException {
		createResource();
		readResource();
	}

	/**
	 * Loads a model from a Draw.io diagram, creates an Excel "report".
	 * @throws IOException
	 */
	@Test
	public void testFromDiagramToExcel() throws IOException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
		
		File diagramFile = new File("diagram.drawio").getCanonicalFile();
		Resource resource = resourceSet.getResource(URI.createFileURI(diagramFile.getAbsolutePath()), true);		
		
		Workbook workbook = ExcelFactory.eINSTANCE.createWorkbook();
		for (EObject root: resource.getContents()) {
			Polity rootPolity = (Polity) root;
			RowSheet rowSheet = workbook.addRowSheet(rootPolity.getName());
			CellRow headerRow = rowSheet.addCellRow();
			headerRow.addStringCell("Constituent");
			headerRow.addStringCell("Residents");
			for (Polity constituent: rootPolity.getConstituents()) {
				CellRow constituentRow = rowSheet.addCellRow();
				constituentRow.addStringCell(constituent.getName());
				for (Person resident: constituent.getResidents()) {
					constituentRow.addStringCell(resident.getName());
				}
			}
		}

		URI excelURI = URI.createFileURI(new File("target/family.xlsx").getAbsolutePath());
		Resource excelResource = resourceSet.createResource(excelURI);
		excelResource.getContents().add(workbook);
		excelResource.save(null);		
	}
		
	@Test
	public void testLoadYaml() throws IOException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(
				ResourceSet.class,
				null,
				new ResourceSetRequirement(
						new NcoreResourceSet(), // Need NcoreResourceSet for resolving references 
						null, 
						null));		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
		
		URI resourceURI = URI.createFileURI(new File("usa.yml").getCanonicalPath());
		Resource resource = resourceSet.getResource(resourceURI, true);
		Polity usa = (Polity) resource.getContents().get(0);
		
		System.out.println(usa.getName());
		System.out.println(usa.getConstituents().size());		

		// Saving for inspection
		URI xmiResourceURI = URI.createFileURI(new File("target/usa.xmi").getCanonicalPath());
		Resource xmiResource = resourceSet.createResource(xmiResourceURI);
		xmiResource.getContents().add(EcoreUtil.copy(usa));
		xmiResource.save(null);				
	}	
	
	/**
	 * Loads a model from a Draw.io diagram, creates an Excel "report".
	 * @throws IOException
	 */
	@Test
	public void testFromDiagramToBinAndGz() throws IOException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
		
		File diagramFile = new File("diagram.drawio").getCanonicalFile();
		Resource diagramResource = resourceSet.getResource(URI.createFileURI(diagramFile.getAbsolutePath()), true);		

		URI binURI = URI.createFileURI(new File("target/family.ebin").getAbsolutePath());
		Resource binResource = resourceSet.createResource(binURI);
		binResource.getContents().addAll(EcoreUtil.copyAll(diagramResource.getContents()));
		binResource.save(null);
		
		URI gzURI = URI.createFileURI(new File("target/family.egz").getAbsolutePath());
		Resource gzResource = resourceSet.createResource(gzURI);
		gzResource.getContents().addAll(EcoreUtil.copyAll(diagramResource.getContents()));
		gzResource.save(null);				
	}
		
	@Test
	@Disabled
	public void testLoadBin() throws IOException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
		
		URI resourceURI = URI.createFileURI(new File("target/family.ebin").getCanonicalPath());
		Resource resource = resourceSet.getResource(resourceURI, true);
		Polity usa = (Polity) resource.getContents().get(0);
		
		System.out.println(usa.getName());
		System.out.println(usa.getConstituents().size());		
	}	
	
	@Test
	@Disabled
	public void testLoadGz() throws IOException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
		
		URI resourceURI = URI.createFileURI(new File("target/family.egz").getCanonicalPath());
		Resource resource = resourceSet.getResource(resourceURI, true);
		Polity usa = (Polity) resource.getContents().get(0);
		
		System.out.println(usa.getName());
		System.out.println(usa.getConstituents().size());		
	}	
	
}
