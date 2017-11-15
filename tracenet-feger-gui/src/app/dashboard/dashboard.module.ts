import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { LabelsDashboardComponent } from './labels-dashboard/labels-dashboard.component';

const dashboardRoutes: Routes = [
  {
    path: '',
    children: [
        {
            path: 'labels-dashboard',
            component: LabelsDashboardComponent
        }
    ]
  }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(dashboardRoutes)
  ],
  exports: [
    RouterModule
  ]
})
export class DashboardModule { }
