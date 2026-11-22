import { OrthographicCamera, WebGLRenderer } from 'three';
import { OfficeAvatar, OfficeWorld } from './office-world';
import { OFFICE_STATUS, OfficeStatuses, officeStatuses } from './office-state';

const WALK_SECONDS = 1.05;
const AISLE_TURN = .64;

export class OfficeRenderer {
  private readonly renderer: WebGLRenderer;
  private readonly world: OfficeWorld;
  private readonly camera = new OrthographicCamera(-1, 1, 1, -1, .1, 80);
  private statuses = officeStatuses(null);
  private runKey = '';
  private visible = false;
  private paused = false;
  private lastFrame = 0;
  private animating = false;

  constructor(canvas: HTMLCanvasElement) {
    const context = canvas.getContext('webgl2', { antialias: false });
    if (!context) throw new Error('WebGL 2 indisponível');
    this.renderer = new WebGLRenderer({ canvas, context, antialias: false });
    this.world = new OfficeWorld();
    this.camera.position.set(12, 13, 18);
    this.camera.lookAt(0, .68, 0);
  }

  resize(width: number, height: number): void {
    if (width <= 0 || height <= 0) return;
    const scale = Math.min(.66, 560 / width);
    const pixelWidth = Math.max(1, Math.round(width * scale));
    const pixelHeight = Math.max(1, Math.round(height * scale));
    this.renderer.setSize(pixelWidth, pixelHeight, false);
    const aspect = pixelWidth / pixelHeight;
    const vertical = Math.max(6.55, 9.75 / aspect);
    this.camera.left = -vertical * aspect;
    this.camera.right = vertical * aspect;
    this.camera.top = vertical;
    this.camera.bottom = -vertical;
    this.camera.updateProjectionMatrix();
    this.draw();
  }

  setStatuses(statuses: OfficeStatuses, runKey: string): void {
    if (runKey !== this.runKey) {
      this.runKey = runKey;
      this.world.avatars.forEach((avatar) => {
        avatar.travel = 0;
        avatar.root.position.copy(avatar.home);
      });
    }
    this.statuses = statuses;
    this.draw();
    this.syncLoop();
  }

  setMotion(visible: boolean, paused: boolean): void {
    if (visible === this.visible && paused === this.paused) return;
    this.visible = visible;
    this.paused = paused;
    this.draw();
    this.syncLoop();
  }

  dispose(releaseContext = true): void {
    this.renderer.setAnimationLoop(null);
    this.world.dispose();
    this.renderer.dispose();
    if (releaseContext) this.renderer.forceContextLoss();
  }

  private readonly frame = (now: number): void => {
    if (this.lastFrame && now - this.lastFrame < 30) return;
    const delta = this.lastFrame ? Math.min(.07, (now - this.lastFrame) / 1000) : 0;
    this.lastFrame = now;
    this.draw(now / 1000, delta);
    this.syncLoop();
  };

  private syncLoop(): void {
    const animate = this.visible && !this.paused && this.world.avatars.some(({ member, travel }) =>
      this.statuses[member.role] === 'RUNNING' || travel > 0);
    if (animate === this.animating) return;
    this.animating = animate;
    this.lastFrame = 0;
    this.renderer.setAnimationLoop(animate ? this.frame : null);
  }

  private draw(time = 0, delta = 0): void {
    if (!this.visible) return;
    for (const [index, avatar] of this.world.avatars.entries()) {
      const status = this.statuses[avatar.member.role];
      const working = status === 'RUNNING';
      this.pose(avatar, working, time + index * .17, delta);
      avatar.beacon.material.color.set(OFFICE_STATUS[status].color);
      avatar.beacon.material.emissive.set(working ? '#216851' : '#000000');
      avatar.screen.material.color.set(working ? '#519a96' : '#364b68');
      if (working && !this.paused) avatar.beacon.rotation.y = time * 1.5;
    }
    this.renderer.render(this.world.scene, this.camera);
  }

  private pose(avatar: OfficeAvatar, working: boolean, time: number, delta: number): void {
    const target = working ? 1 : 0;
    const start = avatar.travel;
    const stride = delta / WALK_SECONDS;
    avatar.travel = this.paused ? target : working ? Math.min(1, start + stride) : Math.max(0, start - stride);
    const { root, home, aisle, desk, travel } = avatar;
    const walking = travel > 0 && travel < 1;
    const previousX = root.position.x;
    const previousZ = root.position.z;
    if (travel < AISLE_TURN) root.position.lerpVectors(home, aisle, travel / AISLE_TURN);
    else root.position.lerpVectors(aisle, desk, (travel - AISLE_TURN) / (1 - AISLE_TURN));
    if (!walking) {
      root.rotation.y = travel === 1 ? 0 : Math.PI;
    } else if (delta > 0) {
      root.rotation.y = Math.atan2(previousX - root.position.x, previousZ - root.position.z);
    }

    const gait = walking ? Math.sin(time * 19) : 0;
    avatar.legs[0].rotation.x = gait * .61;
    avatar.legs[1].rotation.x = -gait * .61;
    const typing = working && travel === 1;
    const keystroke = typing && !this.paused ? Math.sin(time * 23) * .14 : 0;
    avatar.arms[0].rotation.x = typing ? .88 + keystroke : -gait * .46;
    avatar.arms[1].rotation.x = typing ? .88 - keystroke : gait * .46;
    avatar.body.position.y = Math.abs(gait) * .068;
    avatar.beacon.position.y = 2.07 + (typing && !this.paused ? Math.sin(time * 4) * .09 : 0);
  }
}
