import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { TeamRunStep } from './team.service';

@Component({
  selector: 'app-office-scene',
  standalone: true,
  template: '<canvas #scene aria-hidden="true"></canvas>',
  changeDetection: ChangeDetectionStrategy.Eager,
  styles: [':host{display:block;width:100%;height:300px;background:#eaf3f4;border-radius:14px;overflow:hidden}canvas{display:block;width:100%;height:100%}'],
})
export class OfficeSceneComponent implements AfterViewInit,OnChanges,OnDestroy {
  @Input() steps: TeamRunStep[]=[];
  @ViewChild('scene',{static:true}) private canvasRef!: ElementRef<HTMLCanvasElement>;
  private observer?:ResizeObserver;
  private frame=0;
  ngAfterViewInit():void {
    this.observer=new ResizeObserver(()=>this.draw()); this.observer.observe(this.canvasRef.nativeElement);
    this.draw();
  }
  ngOnChanges():void { this.draw(); }
  ngOnDestroy():void { this.observer?.disconnect(); cancelAnimationFrame(this.frame); }
  private draw():void {
    if(!this.canvasRef) return;
    cancelAnimationFrame(this.frame);
    this.frame=requestAnimationFrame(()=>{
      const canvas=this.canvasRef.nativeElement, bounds=canvas.getBoundingClientRect(), ratio=window.devicePixelRatio||1;
      if(!bounds.width||!bounds.height) return;
      canvas.width=bounds.width*ratio;canvas.height=bounds.height*ratio;
      const ctx=canvas.getContext('2d');if(!ctx)return;ctx.scale(ratio,ratio);
      const width=bounds.width,height=bounds.height;
      ctx.fillStyle='#eaf3f4';ctx.fillRect(0,0,width,height);
      ctx.fillStyle='#d7e8e6';ctx.fillRect(0,height*.65,width,height*.35);
      const labels=['PM','Analista','Arquiteto','Dev','QA','Revisor'];
      const roles=['PROJECT_MANAGER','BUSINESS_ANALYST','ARCHITECT','DEVELOPER','QA','REVIEWER'];
      const last=new Map<string,TeamRunStep>();for(const step of this.steps)last.set(step.role,step);
      const gap=width/7, radius=Math.min(24,width/34);
      roles.forEach((role,index)=>{
        const x=gap*(index+1),y=height*.37,step=last.get(role);
        ctx.fillStyle='#c8d7d9';ctx.fillRect(x-36,y+radius+16,72,54);ctx.fillStyle='#91a6a7';ctx.fillRect(x-42,y+radius+12,84,9);
        ctx.fillStyle=step?.status==='RUNNING'?'#e5a233':step?.status==='FAILED'?'#c95353':step?.status==='SUCCEEDED'?'#37866b':step?.status==='SKIPPED'||step?.status==='CANCELLED'?'#788990':'#8b9da3';
        ctx.beginPath();ctx.arc(x,y,radius,0,Math.PI*2);ctx.fill();
        ctx.fillStyle='#fff';ctx.font=`bold ${Math.max(11,radius*.65)}px system-ui`;ctx.textAlign='center';ctx.textBaseline='middle';ctx.fillText(String(index+1),x,y);
        ctx.fillStyle='#163442';ctx.font='600 12px system-ui';ctx.fillText(labels[index],x,y+radius+82);
        ctx.fillStyle='#50666d';ctx.font='11px system-ui';ctx.fillText(step?.status??'IDLE',x,y+radius+99);
      });
      ctx.fillStyle='#335c60';ctx.font='600 13px system-ui';ctx.textAlign='left';ctx.fillText('EQUIPE EM ATIVIDADE',18,24);
    });
  }
}
