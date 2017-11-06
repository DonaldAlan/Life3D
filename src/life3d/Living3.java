package life3d;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
/**
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 *
 */
public class Living3 extends Application {
	private static double rotateDelta=1.0;
	private static long millisecondsToSleepBetweenFrames=1;
	private static final int initialDepth=5;
	private static double newSizeFactor=0.49; // For depth=5, 0.495;
	//--------------
	private int animationCounter=initialDepth;
	private final static int NUMBER_OF_RANDOM_COLORS=200;
	private final static int NUMBER_OF_HSB_COLORS=20;
	private final static int width = 1600;
	private final static int height = 900;
	private static final long ONE_MILLISECOND_IN_NANOSECONDS = 1000L*1000L;
	private final Group root = new Group();
	private final XformWorld world = new XformWorld();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialZ = -800;
	private static double cameraInitialY = 0;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 20000.0;
	private static Random random = new Random();
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private static final Point3D YAXIS = new Point3D(0, 1, 0);
	private static List<PhongMaterial> randomMaterials = new ArrayList<>(NUMBER_OF_RANDOM_COLORS);
	private static List<PhongMaterial> hsbMaterials = new ArrayList<>(NUMBER_OF_HSB_COLORS);
	private volatile Sphere selectedSphere=null;
	private volatile boolean running=true;
	private PointLight light1;
	private PointLight light2;
	private Set<Shape3D> shapes = new HashSet<Shape3D>();
	private Map<Integer,Map<Integer,Map<Integer,Shape3D>>> shapeMap = new HashMap<Integer,Map<Integer,Map<Integer,Shape3D>>>();
	
	private Updateable updateable = createUpdateable(0, 0, 0, 200, initialDepth);
	private static NumberFormat numberFormat = NumberFormat.getInstance();
	Stage primaryStage;
//	private AudioClip machine1 = new AudioClip("file:machine1.mp3");
//	private AudioClip machine2 = new AudioClip("file:machine2.mp3");
//	private AudioClip machine3 = new AudioClip("file:machine3.mp3");
//	private AudioClip machine4 = new AudioClip("file:machine4.mp3");
//	private AudioClip machine5 = new AudioClip("file:machine5.mp3");
//	private AudioClip machine6 = new AudioClip("file:machine6.mp3");
//	private AudioClip clips[] = {machine6,machine5,machine4,machine3,machine2,machine1};
	//--------------
	static {
		randomizeColorsAndMaterials();
	}
	private double maxDistance = 100;
	private double halfMaxDistance = maxDistance/2;
	private interface Updateable {
		void update();
		Node getNode();
	}
	private Point3D getAxis(int depth) {
		switch(depth%6) {
		case 0: return Rotate.X_AXIS;
		case 1: return Rotate.Y_AXIS;
		case 2: return Rotate.Z_AXIS;
		case 3: return Rotate.X_AXIS.multiply(-1);
		case 4: return Rotate.Y_AXIS.multiply(-1);
		case 5: return Rotate.Z_AXIS.multiply(-1);
		default: throw new IllegalStateException();
		}
	}
	private int countUpdateables=0;
	private Updateable createUpdateable(final double x, final double y, final double z, final double size, final int depth) {
		countUpdateables++;
		final int myCountUpdateables=countUpdateables;
		if (countUpdateables%1000==0) {
			System.out.println(countUpdateables + " updateables");
		}
		if (depth==0) {
			final Box box = new Box(size,size,size);
			box.setTranslateX(x);
			box.setTranslateY(y);
			box.setTranslateZ(z);
			box.setRotationAxis(getAxis(depth));
			int distance = (int) (0.1*(Math.abs(x)+Math.abs(y)+ Math.abs(z)));
			PhongMaterial material = hsbMaterials.get(distance%hsbMaterials.size());
			box.setMaterial(material);
			return new Updateable() {
				@Override
				public void update() {
					if (animationCounter==0) {
						box.setRotate(box.getRotate()+rotateDelta); 
					}
				}
				@Override
				public Shape3D getNode() {
					return box;
				}};
		} else {
			final Group group = new Group();
			final List<Updateable> updateables = new ArrayList<Updateable>();
			double halfSize = 0.5*size;
			double newSize = newSizeFactor*size;
			group.setRotationAxis(getAxis(depth+myCountUpdateables));
//			group.setTranslateX(x);
//			group.setTranslateY(y);
//			group.setTranslateZ(z);
			for(double dx=-halfSize; dx<=halfSize;dx+=size) {
				for(double dy=-halfSize; dy<=halfSize;dy+=size) {
					for(double dz=-halfSize; dz<=halfSize;dz+=size) {
						Updateable updateable = createUpdateable(x+dx,y+dy,z+dz,newSize, depth-1);
						group.getChildren().add(updateable.getNode());
						updateables.add(updateable);
					}
				}
			}
			return new Updateable() {
				@Override
				public void update() {
					if (depth == animationCounter) {
						group.setRotate(group.getRotate()+rotateDelta); 
					}
					for(Updateable updateable:updateables) {
						updateable.update();
					}
				}
				@Override
				public Node getNode() {
					return group;
				}};
	}
	}
	//---------------------------------
	private static void randomizeColorsAndMaterials() {
		randomMaterials.clear();
		for (int i = 0; i < NUMBER_OF_RANDOM_COLORS; i++) {
			Color color =randomColorExpensive();
			PhongMaterial pm= new PhongMaterial();
			//pm.setSpecularColor(randomColorExpensive());
			pm.setDiffuseColor(color);
			randomMaterials.add(pm);
		}
		for(int i=0;i<NUMBER_OF_HSB_COLORS;i++) {
			Color color =  Color.hsb((360.0*i)/NUMBER_OF_HSB_COLORS, 0.8,0.6); 
			color = new Color(color.getRed(),color.getGreen(),color.getBlue(),0.7);
			PhongMaterial pm= new PhongMaterial();
			pm.setDiffuseColor(color);
			hsbMaterials.add(pm);
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
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

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
		camera.setTranslateZ(cameraInitialZ);
		camera.setTranslateY(cameraInitialY);
		camera.setRotationAxis(YAXIS);
	}

	private void doReleaseOrMouseExit() {
		if (selectedSphere!=null) {
		     selectedSphere.setScaleX(1);
             selectedSphere.setScaleY(1);
             selectedSphere.setScaleZ(1);
             selectedSphere=null;
		} 
	}
	
	private void addLights() {
		light1 = new PointLight(new Color(0.9, 0.6, 0.6, 1)); 
		light1.setTranslateX(-400);
		light1.setTranslateY(300);
		light1.setTranslateZ(-2000);
		world.getChildren().add(light1);

		light2 = new PointLight(new Color(0.5, 0.9, 0.8, 1)); 
		light2.setTranslateX(500);
		light2.setTranslateY(-300);
		light2.setTranslateZ(-500);
		world.getChildren().add(light2);

		AmbientLight ambientLight = new AmbientLight(new Color(0.6, 0.3, 0.4, 1));
		world.getChildren().add(ambientLight);
	}

	private static Color randomColorExpensive() {
		return new Color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.7);
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
//				  selectedSphere = (Sphere) pr.getIntersectedNode();
//				  selectedSphere.setScaleX(bigSize);
//				  selectedSphere.setScaleY(bigSize);
//				  selectedSphere.setScaleZ(bigSize);
			  }
			  if (pr.getIntersectedNode() instanceof Cylinder) {
			  }
		});
		scene.setOnMouseReleased((MouseEvent me) -> {
			doReleaseOrMouseExit();
		});
		scene.setOnMouseDragExited((MouseEvent me) -> {
			doReleaseOrMouseExit();
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
//				cameraXform.ry.setAngle(cameraXform.ry.getAngle() + mouseDeltaX * 0.2);
//				cameraXform.rx.setAngle(cameraXform.rx.getAngle() - mouseDeltaY * 0.2);

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
				case SPACE:
					running = !running;
					if (running) {
						//clips[animationCounter].play();
					} else {
						//clips[animationCounter].stop();
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
				case RIGHT:
					camera.setTranslateX(camera.getTranslateX() - 10);
					break;
				case UP:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() + 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ()+10);
					}
					break;
				case DOWN:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() - 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ()-10);
					}
					break;
				case C:
						world.requestLayout();
						break;
				case I:
						world.getChildren().removeAll(shapes);
						shapes.clear();
						shapeMap.clear();
					break;
				default:
				}
			}
		});
	}

	
	private double oneOrMinusOne() {
		return random.nextBoolean() ? -1.0 : 1.0;
	}
	private void makeTitle() {
		primaryStage.setTitle("Life3D with " + numberFormat.format(shapes.size()) + " nodes");
	}
	//-------
	private double randomPos() {
		return maxDistance*random.nextDouble() - halfMaxDistance;
	}
	//-----------
	private void draw() {
		world.getChildren().add(updateable.getNode());
	}
	
	// Doesn't seem to work
	public void saveAsJpg(Node node, int count) {
	    WritableImage image = node.snapshot(null, null);
	    File file = new File("/tmp/images/image-" + count + ".jpg");
	    try {
	    	BufferedImage bufferedImage =SwingFXUtils.fromFXImage(image, null); 
	        ImageIO.write(bufferedImage, "jpg", file);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	System.exit(1);
	    }
	}
	//-----------------
	private void animate() {		
		final long last[] = {0};
		final AnimationTimer timer= new AnimationTimer() {
			double degrees=0.0;
			@Override
			public void handle(long nowInNanoSeconds) {
				if (!running) {
					return;
				}
				degrees+= rotateDelta;
				if (degrees>=90) {
					//clips[animationCounter].stop();
					animationCounter += -1;
					if (animationCounter<0) {
						animationCounter=initialDepth;
					}
					degrees=0.0;
					//clips[animationCounter].play();
				}
				//animationCounter = (int) ((nowInNanoSeconds/THREE_SECOND_IN_NANOSECONDS)% initialDepth);
//				running=false;
				long diff=nowInNanoSeconds-last[0];
				if (diff<millisecondsToSleepBetweenFrames*ONE_MILLISECOND_IN_NANOSECONDS) {
					return;
				}
				//saveAsJpg(root,imageCounter[0]++);
				last[0]= nowInNanoSeconds;
				updateable.update();
				makeTitle();
				world.requestLayout();
				if (shapes.isEmpty()) {
				}
			}
		};
		timer.start();
	}
	
	//------------
	@Override
	public void start(Stage stage) throws Exception {
//		for(AudioClip clip:clips) {
//			clip.setRate(1);
//		}
		primaryStage=stage;
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);
		Scene scene = new Scene(root, width, height, true);
		scene.setFill(Color.BLACK);
		primaryStage.setTitle("Life3D");
		primaryStage.setScene(scene);
		handleMouse(scene);
		handleKeyEvents(scene);
		buildCamera();
		addLights();
		scene.setCamera(camera);
		primaryStage.show();
		makeTitle();
		draw();
		animate();
	}

	public static void main(String[] args) {
		try {
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
