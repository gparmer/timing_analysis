%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% RTC (settle time computation)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%path = '../../../tasks_pool_50000/';
path = '../../../tasks/workset/';

ratio = [500 5000 50000];   % avgP : ureboot

mode = 'lazy';             %eager or lazy (ondemand)
wkset = 1:1:30;               % 30 schedulable workset
util = 10:10:90;           % total util
thd_num = 50:1:50;         % total thread numbers 20, 50, 1000
objNum = 5:10:5;        % number of objects to be recovered(per task)

% 200ms -- 20us(0.02ms)  -> 10000:1
% 200ms -- 20us(0.02*20)  -> 500:1

% objects to be recovered
for p =1:length(objNum)
    % ratio
    for i = 1:length(ratio)
        fac = 1/ratio(i)*10000;
        scale = fac;  % 20, 2, 0.2 (500 5000 50000)
        % thread numbers
        for j = 1:length(thd_num)
            thdStr = num2str(thd_num(j));
            %total utilization
            for k = 1:1:length(util)
                % ratio is 4:1 according to the measurment (1 object)
                micro_reboot = 0.02*scale;
                obj_recovery = micro_reboot/4;
                
                utilStr = num2str(util(k));
                utilStr = strcat(strcat(thdStr,'/'), strcat(utilStr,'/'));
                final_path = strcat(path, utilStr);
                saved_dir = strcat(num2str(thd_num(j)), num2str(util(k)));
                saved_dir = strcat(mode, saved_dir);
                saved_dir = strcat(num2str(ratio(i)), saved_dir);
                saved_dir = strcat(saved_dir, '_obj');
                saved_dir = strcat(saved_dir, num2str(objNum(p)));    
                saved_dir = strcat(strcat(num2str(util(k)),'/'), saved_dir);  
                saved_dir = strcat(strcat(num2str(ratio(i)),'/'), saved_dir);  
                saved_dir = strcat(strcat(num2str(thd_num(j)),'/'), saved_dir);  
                mkdir(saved_dir);
                for m = 1:length(wkset)
                    if (wkset(m) == 11)
                        continue;
                    end
                    
                    wkStr = num2str(wkset(m));
                    target = wkStr;
                    
                    wkStr = strcat(wkStr, '_util_');
                    wkStr = strcat(wkStr, num2str(util(k)));
                    wkStr = strcat(wkStr,'.0.data');
                    
                    finalFile = strcat(final_path, wkStr);
                    fprintf('%s \n', finalFile);
                    %current work set info
                    fprintf('<<< %s: ', mode);
                    fprintf('tasks %s ', num2str(thd_num(j)));
                    fprintf('utils %s ', num2str(util(k)));
                    fprintf('objec %s ', num2str(objNum(p)));
                    fprintf('ratio %s >>>\n', num2str(ratio(i)));

                    % settle time computation
                    [a0 b0 Ts Pos] = ts_script(finalFile, micro_reboot, obj_recovery, mode, objNum(p));
                    
                    if (Pos == -55)
                        fprintf('File %s not found\n', finalFile);
                        continue;
                    end
                    
                    if (Pos == -99)
                        fprintf('Task set is not found\n');
                        continue;
                    end
                    
                    test = strcat(saved_dir, '/');
                    test = strcat(test, target);
                    fclose(fopen(test, 'w'));
                    
                    dlmwrite(test, [util(k) Ts], ...
                        '-append', 'precision', '%.4f', 'delimiter', ' ')
                    
                end
            end
        end
    end
end

