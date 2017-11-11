package life3d;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
/**
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 * 
 * Design and save shapes to be loaded by the L key in Life3D.java .
 * 
 */
public class ShapeDesigner extends Application {
	private static int n=5;
	private final static int width = 1600;
	private final static int height = 900;
	private final Group root = new Group();
	private final XformWorld world = new XformWorld();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialX = n*5;
	private static double cameraInitialY = n*3;
	private static double cameraInitialZ = -150;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 20000.0;
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private PhongMaterial selectedMaterial = new PhongMaterial();
	//private PhongMaterial unSelectedMaterial = new PhongMaterial();
	private PhongMaterial unSelectedMaterials[] = new PhongMaterial[n];
	private PhongMaterial unSelectedMaterials3D[][][] = makeUnselectedMaterials3D();
	private PhongMaterial cursorMaterial = new PhongMaterial();
	private static final Point3D YAXIS = new Point3D(0, 1, 0);
	private PointLight light1;
	private PointLight light2;
	private Set<MyBox> selectedCubes = new HashSet<MyBox>();
	private MyBox [][][] cubes = new MyBox[n][n][n]; 
	private Stage primaryStage;
	private MyBox cursorShape=null;
	private int cursorShapeX=n-1;
	private int cursorShapeY=n-1;
	private int cursorShapeZ=n-1;
	private static class MyBox extends Box {
		private final int x;
		private final int y;
		private final int z;
		public MyBox(int x,int y, int z) {
			super(4,4,4);
			this.x=x;
			this.y=y;
			this.z=z;
		}
		public int getX() {
			return x;
		}
		public int getY() {
			return y;
		}
		public int getZ() {
			return z;
		}
	}
	// -------------------------
	private static class XformWorld extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

		public XformWorld() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
			rx.setAngle(5);
			ry.setAngle(40);
			rz.setAngle(-1);
		}
	}

	// -------------------------
	private static class XformCamera extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(16, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(-15, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(-5, 0, 0, 0, Rotate.Z_AXIS);

		public XformCamera() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(camera);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateX(cameraInitialX);
		camera.setTranslateY(cameraInitialY);
		camera.setTranslateZ(cameraInitialZ);
		camera.setRotationAxis(YAXIS);
	}



	private void addLights() {
		light1 = new PointLight(new Color(0.7, 0.5, 0.5, 1));
		light1.setTranslateX(-400);
		light1.setTranslateY(300);
		light1.setTranslateZ(-2000);
		world.getChildren().add(light1);

		light2 = new PointLight(new Color(0.5, 0.5, 0.7, 1));
		light2.setTranslateX(500);
		light2.setTranslateY(-300);
		light2.setTranslateZ(-500);
		world.getChildren().add(light2);

		AmbientLight ambientLight = new AmbientLight(new Color(0.5, 0.6, 0.5, 1));
		world.getChildren().add(ambientLight);
	}

	private void handleMouse(Scene scene) {
		scene.setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
			// this is done after clicking and the rotations are apparently
			// performed in coordinates that are NOT rotated with the camera.
			// (pls activate the two lines below for clicking)
			// cameraXform.rx.setAngle(-90.0);
			// cameraXform.ry.setAngle(180.0);
			PickResult pr = me.getPickResult();
			if (pr.getIntersectedNode() instanceof Sphere) {
				// selectedSphere = (Sphere) pr.getIntersectedNode();
				// selectedSphere.setScaleX(bigSize);
				// selectedSphere.setScaleY(bigSize);
				// selectedSphere.setScaleZ(bigSize);
			}
			if (pr.getIntersectedNode() instanceof Cylinder) {
			}
		});
		scene.setOnMouseReleased((MouseEvent me) -> {
		});
		scene.setOnMouseDragExited((MouseEvent me) -> {
		});
		scene.setOnMouseDragged((MouseEvent me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);
			if (me.isPrimaryButtonDown()) {
				// this is done when the mouse is dragged and each rotation is
				// performed in coordinates, that are rotated with the camera.
				// cameraXform.ry.setAngle(cameraXform.ry.getAngle() +
				// mouseDeltaX * 0.2);
				// cameraXform.rx.setAngle(cameraXform.rx.getAngle() -
				// mouseDeltaY * 0.2);

				world.ry.setAngle(world.ry.getAngle() - mouseDeltaX * 0.2);
				world.rx.setAngle(world.rx.getAngle() + mouseDeltaY * 0.2);
			} else if (me.isSecondaryButtonDown()) {
				cameraXform.t.setZ(cameraXform.t.getZ() - mouseDeltaY);
				cameraXform.t.setX(cameraXform.t.getX() - mouseDeltaX);
			}
		});
	}

	private void handleKeyEvents(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				switch (ke.getCode()) {
				case ENTER:
				case SPACE:
					if (selectedCubes.contains(cursorShape)) {
						selectedCubes.remove(cursorShape);
						//cursorShape.setMaterial(unSelectedMaterials3D[cursorShapeX][cursorShapeY][cursorShapeZ]);
						changeIsCursor(cursorShape,true);
					} else {
						selectedCubes.add(cursorShape);
						cursorShape.setMaterial(selectedMaterial);
					}
					break;
				case Q:
					System.exit(0);
					break;
				case R:
					cameraXform.t.setZ(0);
					cameraXform.rx.setAngle(0);
					cameraXform.ry.setAngle(0);
					camera.setTranslateX(0);
					camera.setTranslateY(cameraInitialY);
					camera.setTranslateZ(cameraInitialZ);
					break;
				case LEFT:
					camera.setTranslateX(camera.getTranslateX() + 10);
					break;
				case X:
					if (ke.isShiftDown()) {
						if (cursorShapeX<n-1) {
							changeIsCursor(cursorShape,false);
							cursorShapeX++;
							cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
							changeIsCursor(cursorShape,true);
						}
					} else if (cursorShapeX>0) {
						changeIsCursor(cursorShape,false);
						cursorShapeX--;
						cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
						changeIsCursor(cursorShape,true);
					}
					break;
				case Y:
					if (ke.isShiftDown()) {
						if (cursorShapeY<n-1) {
							changeIsCursor(cursorShape,false);
							cursorShapeY++;
							cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
							changeIsCursor(cursorShape,true);
						}
					} else if (cursorShapeY>0) {
						changeIsCursor(cursorShape,false);
						cursorShapeY--;
						cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
						changeIsCursor(cursorShape,true);
					}
					break;
				case Z:
					if (ke.isShiftDown()) {
						if (cursorShapeZ<n-1) {
							changeIsCursor(cursorShape,false);
							cursorShapeZ++;
							cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
							changeIsCursor(cursorShape,true);
						}
					} else if (cursorShapeZ>0) {
						changeIsCursor(cursorShape,false);
						cursorShapeZ--;
						cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
						changeIsCursor(cursorShape,true);
					}
					break;
				case RIGHT:
					camera.setTranslateX(camera.getTranslateX() - 10);
					break;
				
				case DOWN:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() + 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ() + 10);
					}
					break;
				case UP:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() - 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ() - 10);
					}
					break;
				case C:
					world.getChildren().clear();
					selectedCubes.clear();
					world.requestLayout();
					break;
				case S:
					if (selectedCubes.isEmpty()) {
						break;
					}
					JFileChooser chooser = new JFileChooser();
					if (Life3D.initialDirectory!=null) {
						chooser.setCurrentDirectory(new File(Life3D.initialDirectory));
					}
					FileFilter filter = new FileFilter() {
						@Override
						public boolean accept(File file) {
							return file.getName().endsWith(".shape");
						}

						@Override
						public String getDescription() {
							return "Shape files";
						}};
					chooser.setFileFilter(filter);
					int result=chooser.showSaveDialog(null);
					if (result!=JFileChooser.APPROVE_OPTION) {
						break;
					}
					File file =chooser.getSelectedFile();
					System.out.println(file);
					if (file!=null) {
						String name=file.getName();
						if (!name.endsWith(".shape")) {
							file = new File(file.getAbsolutePath() + ".shape");
						}
						try {save(file);}
						catch (IOException exc) {
							exc.printStackTrace();
						}
					}
					// Save
					break;
				default:
				}
			}
		});
	}

	private void save(File file) throws IOException {
		PrintWriter writer = new PrintWriter(file);
		List<int[]> selectedCoordinates = new ArrayList<>();
		int minX=Integer.MAX_VALUE;
		int minY=Integer.MAX_VALUE;
		int minZ=Integer.MAX_VALUE;
		for(MyBox box: selectedCubes) {
			int x=box.getX();
			int y=box.getY();
			int z=box.getZ();
			selectedCoordinates.add(new int[]{x,y,z});
			if (x<minX) {minX=x;}
			if (y<minY) {minY=y;}
			if (z<minZ) {minZ=z;}
		}
		for(int[] coordinates: selectedCoordinates) {
			int x=coordinates[0]-minX;
			int y=coordinates[1]-minY;
			int z=coordinates[2]-minZ;
			writer.println(x + "," + y + "," + z);
		}
		writer.close();
	}
	
	private void drawUnselected(final int i, int j, int k) {
		// Sphere shape = new Sphere(3);
		MyBox shape = new MyBox(i,j,k);
		//shape.setMaterial(unSelectedMaterials[i]);
		shape.setMaterial(unSelectedMaterials3D[i][j][k]);
		shape.setTranslateX(i * 10);
		shape.setTranslateY(j * 10);
		shape.setTranslateZ(k * 10);
		cubes[i][j][k]=shape;
		world.getChildren().add(shape);
		shape.setOnMouseClicked( event -> {
			if (selectedCubes.contains(shape)) {
				selectedCubes.remove(shape);
				//shape.setMaterial(unSelectedMaterials[i]);
				shape.setMaterial(unSelectedMaterials3D[cursorShapeX][cursorShapeY][cursorShapeZ]);
			} else {
				selectedCubes.add(shape);
				shape.setMaterial(selectedMaterial);
			}
		});
	}


	private void makeTitle() {
		primaryStage.setTitle("Use x, y, or z to navigate; shift for opposite direction. "
				+ "Enter or click on cubes to select/deselect them. S to save.  Use arrow keys or drag mouse to navigate view.");
	}

	private void drawCubes() {
		for(int i=0;i<n;i++) {
			for(int j=0;j<n;j++) {
				for(int k=0;k<n;k++) {
					drawUnselected(i, j, k);
				}
			}
		}
		cursorShape=cubes[cursorShapeX][cursorShapeY][cursorShapeZ];
		changeIsCursor(cursorShape,true);
	}


	private void changeIsCursor(Shape3D shape, boolean isCursor) {
		if (isCursor) {
			shape.setMaterial(cursorMaterial);
		} else if (selectedCubes.contains(shape)) {
			shape.setMaterial(selectedMaterial);
		} else {
			//shape.setMaterial(unSelectedMaterials[cursorShapeX]);
			shape.setMaterial(unSelectedMaterials3D[cursorShapeX][cursorShapeY][cursorShapeZ]);
		}
	}
	
	private PhongMaterial[][][] makeUnselectedMaterials3D() {
		PhongMaterial[][][] result= new PhongMaterial[n][n][n];
		int n2=2*n;
		for(int x=0;x<n;x++) {
			final double xx=x;
			for(int y=0;y<n;y++) {
				final double yy=y;
				for(int z=0;z<n;z++) {
					final double zz=z;
					result[x][y][z] = new PhongMaterial(new Color(0.2+0.05*(xx/n),0.2+0.05*(yy/n),0.2+0.05*(zz/n),0.1+x/(n+4.0)));
				}
			}
		}
		return result;
	}
	// ------------
	@Override
	public void start(Stage stage) throws Exception {
		
		selectedMaterial.setDiffuseColor(new Color(0.6,0.4,0.7,0.5));
		for(int i=0;i<n;i++) {
			unSelectedMaterials[i]= new PhongMaterial(new Color(0.2,0.2,0.2,0.1+i/(n+4.0)));
		}
		
		primaryStage = stage;
		cursorMaterial.setDiffuseColor(new Color(0.8,0.8,0.5,0.8));
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);
		Scene scene = new Scene(root, width, height, true);
		scene.setFill(Color.DIMGREY);
		primaryStage.setTitle("Life3D");
		primaryStage.setScene(scene);
		handleMouse(scene);
		handleKeyEvents(scene);
		buildCamera();
		addLights();
		scene.setCamera(camera);
		primaryStage.show();
		// drawSquare3();
		drawCubes();
		makeTitle();
	}

	public static void main(String[] args) {
		try {
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
