import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LabelsDashboardComponent } from './labels-dashboard.component';

describe('LabelsDashboardComponent', () => {
  let component: LabelsDashboardComponent;
  let fixture: ComponentFixture<LabelsDashboardComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LabelsDashboardComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LabelsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
