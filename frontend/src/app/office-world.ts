import {
  AmbientLight, BoxGeometry, Color, DirectionalLight, Group, Mesh, MeshLambertMaterial, Scene, Vector3,
} from 'three';
import { OFFICE_CREW } from './office-state';

type CrewMember = (typeof OFFICE_CREW)[number];

export interface OfficeAvatar {
  member: CrewMember;
  root: Group;
  body: Group;
  arms: [Group, Group];
  legs: [Group, Group];
  beacon: Mesh<BoxGeometry, MeshLambertMaterial>;
  screen: Mesh<BoxGeometry, MeshLambertMaterial>;
  home: Vector3;
  aisle: Vector3;
  desk: Vector3;
  travel: number;
}

const WALL = '#2a3a5d';
const EDGE = '#182640';
const TRIM = '#415477';
const WOOD = '#ac795b';

export class OfficeWorld {
  readonly scene = new Scene();
  readonly avatars: OfficeAvatar[];
  private readonly geometry = new BoxGeometry();
  private readonly materials = new Map<string, MeshLambertMaterial>();

  constructor() {
    this.scene.background = new Color('#131c33');
    const key = new DirectionalLight('#e8edff', 2.1);
    key.position.set(5, 12, 8);
    this.scene.add(new AmbientLight('#bac8ff', 1.7), key);
    this.buildRoom();
    this.avatars = OFFICE_CREW.map((member, index) =>
      this.station(member, (index % 3 - 1) * 4.25, index < 3 ? -2.65 : 1.28, index));
  }

  dispose(): void {
    this.geometry.dispose();
    this.materials.forEach((material) => material.dispose());
    for (const avatar of this.avatars) {
      avatar.beacon.material.dispose();
      avatar.screen.material.dispose();
    }
  }

  private buildRoom(): void {
    const room = this.scene;
    this.box(room, 0, -.39, 0, 14.7, .64, 10.2, '#202d49');
    this.box(room, 0, -.08, 0, 14.5, .16, 10, '#3a5273');
    for (let i = 0; i < 10; i++) {
      this.box(room, 0, .007, i - 4.5, 14.35, .025, .96, i % 2 ? '#344b69' : '#3b5372');
    }
    this.box(room, 0, .029, .23, 13.62, .02, .64, '#283f61');
    this.box(room, 0, .05, .23, 13.56, .02, .055, '#7485a4');
    this.box(room, 0, 1.34, -4.9, 14.5, 2.68, .2, WALL);
    this.box(room, -7.13, .89, -.85, .19, 1.78, 7.95, WALL);
    this.box(room, 0, 2.68, -4.9, 14.5, .09, .26, TRIM);
    this.box(room, -7.13, 1.8, -.85, .24, .09, 7.95, TRIM);
    this.box(room, 0, .19, -4.76, 14.5, .16, .11, EDGE);
    this.box(room, -7, .18, -.86, .11, .16, 7.95, EDGE);

    for (const x of [-4.35, 0, 4.35]) {
      this.box(room, x, 1.55, -4.75, 3.33, 1.57, .1, EDGE);
      this.box(room, x, 1.55, -4.68, 3.07, 1.27, .07, '#497aab');
      this.box(room, x, 1.33, -4.62, 2.97, .64, .03, '#537db0');
      this.box(room, x + .77, 1.82, -4.61, 1.32, .55, .03, '#709fc4');
      this.box(room, x, 1.55, -4.58, .07, 1.31, .045, '#263c60');
      this.box(room, x, .87, -4.58, 3.35, .08, .21, '#6c81a0');
    }

    this.box(room, -5.27, .15, 4.28, 2.6, .26, .77, '#33476a');
    this.box(room, -5.27, .41, 4.43, 2.53, .43, .55, '#796fac');
    this.box(room, -5.27, .55, 4.73, 2.6, .63, .22, '#a090c4');
    this.box(room, -4.56, .64, 4.39, .47, .27, .34, '#eb9a88');
    this.box(room, 5.33, .39, 4.32, 1.8, .79, .67, '#515b85');
    this.box(room, 5.33, .82, 4.32, 1.91, .09, .77, '#7e87a8');
    for (let book = 0; book < 5; book++) {
      this.box(room, 4.75 + book * .15, .97 + (book % 2) * .04, 4.32, .11, .24, .28,
        OFFICE_CREW[book].color);
    }
    for (const [x, z] of [[-6.55, -4.02], [6.54, -4.07], [6.75, 3.7]]) this.plant(x, z);
  }

  private plant(x: number, z: number): void {
    this.box(this.scene, x, .2, z, .55, .4, .55, '#bd7a72');
    this.box(this.scene, x, .61, z, .12, .55, .13, '#446d5b');
    this.box(this.scene, x - .13, .88, z, .47, .36, .32, '#5bb899');
    this.box(this.scene, x + .15, 1.04, z - .06, .26, .38, .34, '#79c4a1');
    this.box(this.scene, x + .17, .77, z + .14, .4, .22, .25, '#448972');
  }

  private station(member: CrewMember, x: number, z: number, index: number): OfficeAvatar {
    const room = this.scene;
    this.box(room, x, .042, z + .6, 3.56, .038, 2.23, '#465b7b');
    this.box(room, x, .844, z, 2.64, .17, 1.08, WOOD);
    this.box(room, x, .9, z + .54, 2.65, .072, .045, member.color);
    for (const side of [-1, 1]) this.box(room, x + side * 1.05, .397, z, .16, .75, .83, EDGE);
    this.box(room, x - .11, .98, z - .17, .62, .09, .3, '#23334d');
    this.box(room, x - .11, 1.12, z - .21, .11, .25, .1, EDGE);
    this.box(room, x - .11, 1.42, z - .22, 1.06, .63, .16, '#152139');
    const screen = this.box(room, x - .11, 1.42, z - .13, .91, .47, .025, '#364b68');
    screen.material = screen.material.clone();
    for (const [line, width] of [.52, .32, .43].entries()) {
      this.box(room, x - .51 + width / 2, 1.55 - line * .105, z - .111, width, .038, .019, member.color);
    }
    this.box(room, x - .11, .961, z + .28, .81, .058, .22, '#27364e');
    this.box(room, x + .55, .971, z + .31, .17, .073, .19, '#7587a5');
    this.box(room, x + .98, 1.022, z + .02, .24, .23, .23, member.color);
    this.box(room, x + 1.1, 1.048, z + .02, .07, .12, .1, member.color);

    const root = new Group();
    const body = new Group();
    root.add(body);
    room.add(root);
    this.box(body, 0, 1.02, 0, .48, .5, .32, member.color);
    this.box(body, 0, .775, 0, .49, .11, .335, '#293954');
    this.box(body, 0, 1.51, 0, .43, .43, .39, member.skin);
    this.box(body, 0, 1.73, .018, .46, .17, .425, member.hair);
    this.box(body, index % 2 ? -.18 : .16, 1.63, -.17, .125, .19, .115, member.hair);
    if (index === 1 || index === 4) this.box(body, 0, 1.52, .19, .45, .42, .12, member.hair);
    for (const side of [-1, 1]) this.box(body, side * .091, 1.54, -.207, .037, .041, .022, '#252333');
    const limb = (side: number, arm: boolean) => {
      const joint = new Group();
      joint.position.set(side * (arm ? .33 : .132), arm ? 1.22 : .77, 0);
      body.add(joint);
      this.box(joint, 0, arm ? -.18 : -.3, 0, arm ? .16 : .19, arm ? .41 : .61, .19,
        arm ? member.color : '#26344f');
      this.box(joint, 0, arm ? -.405 : -.669, arm ? 0 : -.054, .18, arm ? .133 : .19,
        arm ? .19 : .3, arm ? member.skin : '#172239');
      return joint;
    };
    const beacon = this.box(root, 0, 2.07, 0, .16, .16, .16, '#8495b1');
    beacon.material = beacon.material.clone();
    beacon.rotation.set(.45, .3, .78);
    const desk = new Vector3(x, .066, z + .96);
    const aisle = new Vector3(x, .066, z + 1.68);
    const home = new Vector3(x + 1.31, .066, z + 1.68);
    root.position.copy(home);
    return { member, root, body, arms: [limb(-1, true), limb(1, true)],
      legs: [limb(-1, false), limb(1, false)], beacon, screen, home, aisle, desk, travel: 0 };
  }

  private box(parent: Group | Scene, x: number, y: number, z: number,
              width: number, height: number, depth: number, color: string) {
    let material = this.materials.get(color);
    if (!material) {
      material = new MeshLambertMaterial({ color });
      this.materials.set(color, material);
    }
    const mesh = new Mesh(this.geometry, material);
    mesh.position.set(x, y, z);
    mesh.scale.set(width, height, depth);
    parent.add(mesh);
    return mesh;
  }
}
