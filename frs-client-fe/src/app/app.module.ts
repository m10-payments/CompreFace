import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { MainLayoutComponent } from './ui/main-layout/main-layout.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { StoreModule } from "@ngrx/store";
import { EffectsModule } from "@ngrx/effects";
import { HTTP_INTERCEPTORS, HttpClientModule } from "@angular/common/http";
import { sharedReducers } from "./store";
import { ErrorInterceptor, TokenInterceptor } from "./core/auth/token.inerceptor";
import { AuthGuard, LoginGuard } from "./core/auth/auth.guard";
import { RouterStateSerializer, StoreRouterConnectingModule } from "@ngrx/router-store";
import { environment } from "../environments/environment";
import { StoreDevtoolsModule } from "@ngrx/store-devtools";
import { ToolBarModule } from "./features/tool-bar/tool-bar.module";
import { AppSerializer } from "./store/router/reducer";
import { AuthEffects } from "./store/auth/effects";
import { ApplciationListEffect } from './store/applicationList/effects';
import { CreateDialogComponent } from 'src/app/features/create-dialog/create-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule, MatInputModule, MatButtonModule } from '@angular/material';
import { ApplicationReducer } from './store/application/reducers';

@NgModule({
  declarations: [
    AppComponent,
    MainLayoutComponent,
    CreateDialogComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatFormFieldModule,
    FormsModule,
    MatDialogModule,
    MatInputModule,
    MatButtonModule,
    ToolBarModule,
    HttpClientModule,
    StoreModule.forRoot(sharedReducers),
    StoreModule.forFeature('application', ApplicationReducer),
    EffectsModule.forRoot([AuthEffects, ApplciationListEffect]),
    StoreRouterConnectingModule.forRoot({
      stateKey: 'router'
    }),
    !environment.production
      ? StoreDevtoolsModule.instrument({ maxAge: 30 })
      : [],
  ],
  providers: [
    AuthGuard,
    LoginGuard,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    { provide: RouterStateSerializer, useClass: AppSerializer }
  ],
  bootstrap: [AppComponent],
  entryComponents: [CreateDialogComponent]
})
export class AppModule { }
