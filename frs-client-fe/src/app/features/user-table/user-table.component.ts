import {Component, OnInit, Input, ChangeDetectionStrategy, Output, EventEmitter} from '@angular/core';
import {TableComponent} from '../table/table.component';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {AppUser} from 'src/app/data/appUser';


@Component({
  selector: 'app-user-table',
  templateUrl: './user-table.component.html',
  styleUrls: ['./user-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserTableComponent extends TableComponent implements OnInit {
  @Input() availableRoles$: Observable<string[]>;
  @Output() deleteUser = new EventEmitter<AppUser>();

  public isRoleChangeAllowed(userRole: string): Observable<boolean> {
    return this.availableRoles$.pipe(map(availableRoles => availableRoles.indexOf(userRole) > -1));
  }

  public delete(user: AppUser): void {
    this.deleteUser.emit(user);
  }
}
