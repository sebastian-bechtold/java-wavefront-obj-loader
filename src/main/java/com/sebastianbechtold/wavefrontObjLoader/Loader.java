package main.java.com.sebastianbechtold.wavefrontObjLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Loader {

	public LoaderResult readFile(String filePathString) {

		LoaderResult result = new LoaderResult();

		boolean yIsUp = false;

		System.out.println("Reading 3D model from .obj file '" + filePathString + "'...");

		File f = new File(filePathString);

		if (!f.exists()) {
			System.out.println("File not found: " + filePathString);
			return null;
		}

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(filePathString));
		} catch (FileNotFoundException e) {
			System.out.println("Failed to create buffered reader for file: " + filePathString);

			return null;
		}

		ArrayList<String> materialFilePaths = new ArrayList<>();
		//ArrayList<String> usedMaterials = new ArrayList<>();

		ArrayList<double[]> vertices = new ArrayList<>();
		ArrayList<double[]> normals = new ArrayList<>();
		ArrayList<double[]> texcoords = new ArrayList<>();
		ArrayList<double[]> colors = new ArrayList<>();

		String currentMaterial = "default";

		String line;

		LoaderObject currentObject = null;// new LoaderObject();

		try {
			while ((line = br.readLine()) != null) {

				line = line.trim();

				// ############## BEGIN Skip empty lines and comments ##############
				if (line.length() == 0) {
					continue;
				}

				if (line.substring(0, 1).equals("#")) {
					continue;
				}
				// ############## END Skip empty lines and comments ##############

				// Split line into pieces.
				String[] lineParts = line.split("\\s+");

				// ############################### BEGIN Read vertex ##########################
				if (lineParts[0].equals("v") && lineParts.length >= 4) {

					// ################ BEGIN Read vertex position #################

					double x = 0, y = 0, z = 0;

					if (yIsUp) {
						x = Double.parseDouble(lineParts[1]);
						y = -Double.parseDouble(lineParts[3]);
						z = Double.parseDouble(lineParts[2]);
					} else {
						x = Double.parseDouble(lineParts[1]);
						y = Double.parseDouble(lineParts[2]);
						z = Double.parseDouble(lineParts[3]);
					}

					double[] v = { x, y, z };
					vertices.add(v);
					// ################ END Read vertex position #################

					// ############### BEGIN Read vertex color ##################
					if (lineParts.length >= 7) {
						float r = 1, g = 1, b = 1;

						r = Float.parseFloat(lineParts[4]);
						g = Float.parseFloat(lineParts[5]);
						b = Float.parseFloat(lineParts[6]);

						double[] c = { r, g, b };
						colors.add(c);
					}
					// ############### END Read vertex color ##################
				}
				// ########################## END Read vertex ########################

				// ########################### BEGIN Read normal vector #######################
				else if (lineParts[0].equals("vn") && lineParts.length >= 4) {

					double x = 0, y = 0, z = 0;

					if (yIsUp) {
						x = Double.parseDouble(lineParts[1]);
						y = -Double.parseDouble(lineParts[3]);
						z = Double.parseDouble(lineParts[2]);
					} else {
						x = Double.parseDouble(lineParts[1]);
						y = Double.parseDouble(lineParts[2]);
						z = Double.parseDouble(lineParts[3]);
					}

					double[] n = { x, y, z };
					normals.add(n);
				}
				// ############################# END Read normal vector ##############################

				// ###################### BEGIN Read texture coordinates ##########################
				else if (lineParts[0].equals("vt") && lineParts.length >= 3) {

					double[] tc = { Double.parseDouble(lineParts[1]), Double.parseDouble(lineParts[2]) };
					texcoords.add(tc);
				}
				// ###################### END Read texture coordinates ##########################

				// ################################# BEGIN Read faces ################################
				else if (lineParts[0].equals("f")) {

					// Skip line if it hasn't the correct structure to define a face (triangle or quad):
					if (lineParts.length < 4 || lineParts.length > 5) {
						System.out.println("Unsupported primitive!");
						continue;
					}

					// A face can have four vertices at most:
					LoaderVertex[] v = new LoaderVertex[4];

					// ################## BEGIN Loop over definitions of face vertices ###################
					for (int ii = 0; ii < lineParts.length - 1; ii++) {

						v[ii] = new LoaderVertex();

						String[] sv = lineParts[ii + 1].split("/");

						// ############# BEGIN Read position/normal/texture indices #############
						int vidx = 0;
						int tidx = 0;
						int nidx = 0;

						try {
							vidx = Integer.parseInt(sv[0]);
						} catch (Exception e) {
						}

						try {
							tidx = Integer.parseInt(sv[1]);
						} catch (Exception e) {
						}

						try {
							nidx = Integer.parseInt(sv[2]);
						} catch (Exception e) {
						}
						// ############# END Read position/normal/texture indices #############

						// ############# BEGIN Set position/normal/texture references #############

						// Set vertex position:
						if (vidx >= 1 && vertices.size() >= vidx) {
							v[ii].mPos = vertices.get(vidx - 1);
						}

						// Set vertex normal:
						if (nidx >= 1 && normals.size() >= nidx) {
							v[ii].mNormal = normals.get(nidx - 1);
						}

						// Set vertex texture coordinates:
						if (tidx >= 1 && texcoords.size() >= tidx) {
							v[ii].mTex = texcoords.get(tidx - 1);
						}

						// ############# END Set position/normal/texture references #############
					}
					// ################## END Loop over definitions of face vertices ###################

					// ################## BEGIN Read a triangle ###################
					if (lineParts.length >= 4) {
						LoaderVertex[] tri = { v[0], v[1], v[2] };
						// LoaderTriangle shape = new LoaderTriangle(tri);

						currentObject.mFaces.add(tri);
					}
					// ################## END Read a triangle ###################

					// ################## BEGIN Read a quad (second triangle) ###################
					if (lineParts.length == 5) {
						LoaderVertex[] tri = { v[0], v[2], v[3] };
						// LoaderTriangle shape = new LoaderTriangle(tri);

						currentObject.mFaces.add(tri);
					}
					// ################## END Read a quad (second triangle) ###################
				}
				// ############################# END Read faces ################################

				// Read material file reference:
				else if (lineParts[0].equals("mtllib")) {
					materialFilePaths.add(lineParts[1].trim());

					// TODO 2: Support multiple material file references?
					result.mMaterialsFilePath = lineParts[1].trim();
				}

				// Read current material:
				else if (lineParts[0].equals("usemtl")) {

					if (currentObject != null) {
						// System.out.println("Object read with " + currentObject.mFaces.size() + " faces");
						currentObject.mMaterial = currentMaterial;
						result.mObjects.add(currentObject);
					}

					currentObject = new LoaderObject();

					currentMaterial = lineParts[1].trim();
					//usedMaterials.add(lineParts[1].trim());

				}

				// What?
				else if (lineParts[0].equals("s")) {
					// TODO 2: What?
				}

				// Handle unknown lines:
				else {
					System.out.println("Unknown line: " + line);
				}
			}

			// Don't forget to add last object:

			if (currentObject != null) {
				// System.out.println("Object read with " + currentObject.mFaces.size() + " faces");
				currentObject.mMaterial = currentMaterial;
				result.mObjects.add(currentObject);
			}

			// System.out.println("# faces loaded: " + result.size());

		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return result;
	}
}
